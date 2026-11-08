package com.nolleo.app.app

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.SourceMode
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.navi.DestinationPreset
import com.nolleo.app.core.navi.TripEvent
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.core.speech.AnswerInput
import com.nolleo.app.core.speech.FocusChange
import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.ScriptedSpeechEngine
import com.nolleo.app.core.speech.SpeechEngineKind
import com.nolleo.app.core.speech.SpeechMode
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.data.AppSettings
import com.nolleo.app.domain.drowsy.DriverBaseline
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.engine.GameSession
import com.nolleo.app.domain.games.IdiomScreenGame
import com.nolleo.app.domain.games.RegionScreenQuiz
import com.nolleo.app.domain.games.WordChainScreenGame
import com.nolleo.app.domain.engine.SessionCallbacks
import com.nolleo.app.domain.engine.SessionInputs
import com.nolleo.app.domain.engine.SessionPhase
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.domain.engine.SessionSnapshot
import com.nolleo.app.domain.engine.SessionState
import com.nolleo.app.domain.engine.SessionSummary
import com.nolleo.app.domain.engine.SpeechPort
import com.nolleo.app.domain.model.DriveState
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.GearPosition
import com.nolleo.app.domain.model.PauseReason
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.RoundPacing
import com.nolleo.app.domain.model.SessionPlan
import com.nolleo.app.domain.model.TripSourceKind
import com.nolleo.app.domain.rest.RestStopSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverProfile(val id: Int, val name: String)

data class HomeState(
    val presetId: String? = null,
    /** 내비에 이미 설정된 목적지로 출발 */
    val useNaviRoute: Boolean = false,
    val driverId: Int = 1,
    val passengerCount: Int = 1,
    val choice: GameChoice = GameChoice.ALTERNATE,
    /** 판 간격 (여유 있게 / 자주 / 쉬지 않고) */
    val pacing: RoundPacing = RoundPacing.RELAXED,
) {
    /**
     * 내비 길 안내가 새로 잡히면 그 목적지를 기본으로 고르고, 길 안내가 끝나면 프리셋으로 돌아간다.
     * [autoSelect] = false(디버그 패널 여정=시뮬, 시연용)면 카드만 보여 주고 고르지는 않는다 — 고르면 시뮬 여정이 아니게 된다.
     */
    fun withNaviRoute(available: Boolean, autoSelect: Boolean = true): HomeState = when {
        available && autoSelect && !useNaviRoute -> copy(useNaviRoute = true)
        !available && useNaviRoute -> copy(useNaviRoute = false)
        else -> this
    }
}

data class PlanPreview(val text: String, val plan: SessionPlan, val remainSec: Int, val destinationName: String)

/**
 * 앱 계층의 오케스트레이터: 홈 설정 → 세션 시작, 차량·내비·위치 SDK 입력을 GameSession 에 연결하고
 * 오디오 포커스·TBT 자동 일시정지·백그라운드·상태 저장을 처리한다.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameCoordinator(private val c: AppContainer) {

    private val scope = c.appScope
    private val mode = c.modeController

    val drivers = listOf(DriverProfile(1, "아빠"), DriverProfile(2, "엄마"), DriverProfile(3, "운전자"))
    private val seatNames = listOf("조수석", "뒷좌석 왼쪽", "뒷좌석 오른쪽")

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _home = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _session = MutableStateFlow<GameSession?>(null)
    val session: StateFlow<GameSession?> = _session.asStateFlow()

    val sessionState: StateFlow<SessionState?> = _session
        .flatMapLatest { s -> s?.state ?: flowOf(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _summary = MutableStateFlow<SessionSummary?>(null)
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()

    /** 졸음 1단계 카드("잠깐 쉬어 갈까요?")를 보이는 중 — [STAGE1_CARD_MS] 뒤 저절로 닫힌다 */
    private val _stage1CardVisible = MutableStateFlow(false)
    val stage1CardVisible: StateFlow<Boolean> = _stage1CardVisible.asStateFlow()

    private val _restorable = MutableStateFlow<SessionSnapshot?>(null)
    val restorable: StateFlow<SessionSnapshot?> = _restorable.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * 정차 중 화면 게임 — 사자성어·지역 퀴즈(지금 있는 시/도)·끝말잇기(이어질 말 고르기). 주행 중 음성 판과 따로 놀고,
     * 세션 동안 게임마다 점수를 유지한다.
     */
    val idiomQuiz = ScreenQuizController(c, scope, ScreenGameKind.IDIOM, "IdiomQuiz") {
        c.content.content.value?.idioms?.let { IdiomScreenGame(it) }?.takeIf { it.isAvailable }
    }
    val regionQuiz = ScreenQuizController(c, scope, ScreenGameKind.REGION, "RegionQuiz") {
        c.content.content.value?.let { RegionScreenQuiz(it, { c.locationRepository.regionCode.value }) }?.takeIf { it.isAvailable }
    }
    val wordQuiz = ScreenQuizController(c, scope, ScreenGameKind.WORD, "WordQuiz") {
        c.content.dictionary.value?.let { WordChainScreenGame(it) }?.takeIf { it.isAvailable }
    }
    val screenQuizzes: List<ScreenQuizController> = listOf(idiomQuiz, regionQuiz, wordQuiz)

    fun screenQuiz(kind: ScreenGameKind): ScreenQuizController = screenQuizzes.first { it.kind == kind }

    private fun stopScreenQuizzes(reason: String) = screenQuizzes.forEach { it.stop(reason) }

    private val arrived = MutableStateFlow(false)
    private var sessionJob: Job? = null
    private var stage1HideJob: Job? = null
    private var tbtClearJob: Job? = null
    private var backgroundInterrupted = false
    private var activeDriverId: Int = 1

    // ── GameSession 입력 ─────────────────────────────────────────
    val gameMode: StateFlow<GameMode> = mode.decision.map { it.mode }.stateIn(scope, SharingStarted.Eagerly, GameMode.IDLE)
    private val screenAvailable = mode.decision.map { it.screenAvailable }.stateIn(scope, SharingStarted.Eagerly, false)
    private val driveState = mode.decision.map { it.driveState }.stateIn(scope, SharingStarted.Eagerly, mode.decision.value.driveState)
    private val remainSec: StateFlow<Int?> = combine(c.tripRepository.snapshot, arrived) { snap, done ->
        if (done) 0 else snap?.remainTimeSec
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** 졸음 케어 2단계의 쉬어 가기 (경로 위 휴게소·졸음쉼터 고르기·도착 감지) */
    val restCare = RestCareController(c, scope, driveState)

    private val inputs = SessionInputs(
        mode = gameMode,
        screenAvailable = screenAvailable,
        remainSec = remainSec,
        regionCode = c.locationRepository.regionCode,
    )

    /** 홈 화면 플랜 미리보기 ("4시간 10분 → 5판") */
    val planPreview: StateFlow<PlanPreview?> = combine(_home, c.naviTrip.activeNaviDestination, _settings) { h, navi, s ->
        val (remain, name) = when {
            h.useNaviRoute && navi?.remainTimeSec != null -> navi.remainTimeSec to navi.destinationName
            else -> DestinationPreset.byId(h.presetId)?.let { it.estimatedSec to it.poiName } ?: return@combine null
        }
        val plan = SessionPlanner.plan(remain, h.choice, s.demoMode, h.pacing)
        PlanPreview(SessionPlanner.previewText(remain, plan, TripUnits::formatDuration), plan, remain, name)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** 착좌 센서(Pleos Vehicle Seat)로 감지한 동승자 수 — 홈에서 자동 제안 */
    val detectedPassengers: StateFlow<Int?> = c.vehicleSdk.seatOccupancy
        .map { m -> if (m.isEmpty()) null else m.values.count { it } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Pleos STT 가 안 되고 Android 인식기가 마이크 퍼미션만 기다리는 상태 */
    val needsMicPermission: StateFlow<Boolean> = combine(
        c.pleosSpeech.sttStatus, c.androidSpeech.needsMicPermission, c.speechRouter.mode,
    ) { pleosStt, androidNeeds, m ->
        androidNeeds && !pleosStt.isAvailable && pleosStt !is SdkStatus.Connecting && m != SpeechMode.SCRIPTED
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val speechPort = object : SpeechPort {
        override suspend fun speak(text: String) {
            // 음성 게임이 말하기 시작하면 화면 게임의 읽기는 먼저 멈추고 포커스를 넘긴다
            stopScreenQuizzes("음성 게임 발화")
            if (!ensureFocus()) return
            c.speechRouter.speak(text)
        }

        override fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = c.speechRouter.listen(timeoutMs, hint)

        override fun stop() = c.speechRouter.stopAll()

        override fun markNewStream() = c.speechRouter.markNewStream()
    }

    private var initialized = false

    /**
     * 앱 수명 동안 한 번만. 작업 제거 뒤 같은 프로세스에서 다시 열면 [AppContainer.start] 가 다시 불리지만,
     * 아래 구독을 또 붙이면 포커스 손실·TBT 처리가 두 번씩 돈다 (SDK 초기화만 다시 한다).
     */
    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch {
            val s = c.preferences.loadSettings()
            _settings.value = s
            applySettings(previous = null, s)
            _home.value = HomeState(
                presetId = s.lastPresetId,
                driverId = s.lastDriverId,
                passengerCount = s.lastPassengerCount.coerceIn(0, 3),
                choice = s.lastChoice,
                pacing = s.lastPacing,
            ).withNaviRoute(c.naviTrip.activeNaviDestination.value != null, autoSelect = s.tripMode != SourceMode.SIM)
            c.content.load()
            val saved = c.preferences.loadSession()
            if (saved != null && c.clock.wallMs() - saved.savedAtWallMs < RESTORE_MAX_AGE_MS) {
                _restorable.value = saved
                AppLog.i(TAG, "restorable session found (round ${saved.roundIndex + 1})")
            } else if (saved != null) {
                c.preferences.saveSession(null)
            }
        }
        restCare.onArrived = { s ->
            _session.value?.announce("${s.name}에 도착했어요. 차를 세우고 충분히 쉬었다 가요.", holdAfter = true)
        }
        restCare.onRested = { _session.value?.onRestFinished() }
        observeFocus()
        observeFocusRequest()
        observeTrip()
        observeNaviRoute()
        observeTbt()
        observePauseFocusPolicy()
        // 출발 등으로 화면 게임을 벗어나면 화면 게임 읽기를 즉시 멈춘다
        gameMode.onEach { m -> if (m != GameMode.SCREEN) stopScreenQuizzes("mode=${m.name}") }.launchIn(scope)
        // 시뮬 여정은 주차 중에, 그리고 쉬어 가기를 묻는 동안 멈춘다 — 30배속이라 질문하는 15초에 11 km 를 지나
        // 권한 쉼 장소를 수락 전에 지나쳐 버리기 때문 (실제 내비는 해당 없음)
        combine(driveState, restCare.state) { d, rest -> d != DriveState.PARKED && rest !is RestCareController.State.Offered }
            .onEach { c.simTrip.moving.value = it }
            .launchIn(scope)
    }

    /**
     * 바뀐 설정만 적용한다. 응답 입력이 그대로인데 대본을 다시 읽으면 대본 순서가 처음으로 돌아가고,
     * 배율이 그대로인데 덮어쓰면 프리셋 배율(근처 10분 = 2배속)로 시작한 여정이 저장된 배율로 바뀐다.
     * @param previous null = 앱 시작 (모두 적용)
     */
    private fun applySettings(previous: AppSettings?, s: AppSettings) {
        c.carRepository.mode.value = s.carMode
        c.tripRepository.mode.value = s.tripMode
        c.locationRepository.mode.value = s.locationMode
        c.speechRouter.mode.value = s.speechMode
        if (previous?.answerInput != s.answerInput) setAnswerInputInternal(s.answerInput)
        if (previous?.simTimeScale != s.simTimeScale) c.simTrip.timeScale.value = s.simTimeScale
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val previous = _settings.value
        val next = transform(previous)
        if (next == previous) return
        _settings.value = next
        applySettings(previous, next)
        scope.launch { c.preferences.saveSettings(next) }
    }

    // ── 홈 ──────────────────────────────────────────────────────

    fun selectPreset(id: String) = _home.update { it.copy(presetId = id, useNaviRoute = false) }
    fun selectNaviRoute() = _home.update { it.copy(useNaviRoute = true) }
    fun selectDriver(id: Int) = _home.update { it.copy(driverId = id) }
    fun setPassengers(count: Int) = _home.update { it.copy(passengerCount = count.coerceIn(0, 3)) }
    fun selectChoice(choice: GameChoice) = _home.update { it.copy(choice = choice) }
    fun selectPacing(pacing: RoundPacing) = _home.update { it.copy(pacing = pacing) }
    fun dismissMessage() {
        _message.value = null
    }

    private fun buildPlayers(h: HomeState): List<Player> {
        val driverName = drivers.firstOrNull { it.id == h.driverId }?.name ?: "운전자"
        val driver = Player(id = 1, name = driverName, isDriver = true)
        val passengers = (0 until h.passengerCount).map { i -> Player(id = 2 + i, name = seatNames[i], isDriver = false) }
        return listOf(driver) + passengers
    }

    fun startSession() {
        if (_session.value != null || starting) return
        val content = c.content.content.value
        val dictionary = c.content.dictionary.value
        if (content == null || dictionary == null) {
            _message.value = "콘텐츠를 불러오는 중이에요. 잠시 후 다시 눌러 주세요."
            return
        }
        val h = _home.value
        val preview = planPreview.value ?: run {
            _message.value = "목적지를 먼저 골라 주세요."
            return
        }
        // 남은 시간이 도착 여유(5분) 이하면 출발하자마자 "곧 도착"으로 끝난다
        if (preview.remainSec <= SessionPlanner.END_BUFFER_SEC) {
            _message.value = SessionPlanner.TOO_CLOSE_MESSAGE
            return
        }
        // 내비 목적지를 골랐어도 그 사이 길 안내가 끝났으면 프리셋으로 간다 (planPreview 와 같은 기준)
        val useNavi = h.useNaviRoute && c.naviTrip.activeNaviDestination.value?.remainTimeSec != null
        val preset = DestinationPreset.byId(h.presetId)
        val demo = _settings.value.demoMode
        val players = buildPlayers(h)
        activeDriverId = h.driverId
        updateSettings { it.copy(lastPresetId = h.presetId ?: it.lastPresetId, lastDriverId = h.driverId, lastPassengerCount = h.passengerCount, lastChoice = h.choice, lastPacing = h.pacing) }
        c.scriptedSpeech.rewind()
        prepareSessionStart()
        launchStart {
            val baseline = c.preferences.loadBaseline(h.driverId)
                ?: if (demo) DEMO_BASELINE.also { AppLog.i(TAG, "demo: seeded driver baseline ${it.latencyEmaMs.toLong()}ms") } else null
            arrived.value = false
            if (useNavi) c.tripRepository.adoptNaviRoute() else preset?.let { c.tripRepository.beginTrip(it) }
            val session = GameSession(
                speech = speechPort,
                clock = c.clock,
                content = content,
                dictionary = dictionary,
                players = players,
                initialPlan = preview.plan,
                choice = h.choice,
                demo = demo,
                drowsy = DrowsinessMonitor(baseline, drowsyConfig(demo)),
                inputs = inputs,
                callbacks = callbacks,
                destinationName = preview.destinationName,
                presetId = if (useNavi) null else preset?.id,
            )
            AppLog.i(TAG, "session start → ${preview.text}")
            launchSession(session)
        }
    }

    /**
     * 출발·이어서 하기를 눌러 세션을 만드는 중(기준값 읽기 등) — 그 사이 다시 눌러도 세션이 둘 생기지 않게 한다.
     */
    private var starting = false

    private fun launchStart(block: suspend () -> Unit) {
        starting = true
        scope.launch {
            try {
                block()
            } finally {
                starting = false
            }
        }
    }

    /** 출발·이어서 하기 공통 준비 */
    private fun prepareSessionStart() {
        // 실패했던 음성 엔진은 새 세션에서 한 번 다시 초기화한다 (예: 마이크를 켜지 않은 채 생긴 MIC_ERROR)
        c.speechRouter.recoverUnavailableEngines()
        // 끝말잇기 검증용 사전(약 19만 어)은 첫 판을 시작할 때 백그라운드에서 붙인다 — 세션 시작을 기다리게 하지 않는다
        scope.launch { c.content.loadValidWords() }
    }

    fun resumeSaved() {
        if (_session.value != null || starting) return
        val snap = _restorable.value ?: return
        val content = c.content.content.value ?: return
        val dictionary = c.content.dictionary.value ?: return
        _restorable.value = null
        prepareSessionStart()
        launchStart {
            // v1 저장본에는 운전자 id 가 없다 → 운전자 이름으로 찾는다
            activeDriverId = snap.driverProfileId
                ?: drivers.firstOrNull { d -> snap.players.firstOrNull { it.isDriver }?.name == d.name }?.id ?: 1
            val baseline = c.preferences.loadBaseline(activeDriverId) ?: if (snap.demo) DEMO_BASELINE else null
            val monitor = DrowsinessMonitor(baseline, drowsyConfig(snap.demo)).also { it.restore(snap.drowsy) }
            arrived.value = false
            DestinationPreset.byId(snap.presetId)?.let { c.tripRepository.beginTrip(it) } ?: c.tripRepository.adoptNaviRoute()
            // 시뮬 여정은 저장할 때의 남은 시간부터 이어 간다 (앱이 꺼져 있던 동안은 흐르지 않은 것으로 본다)
            if (c.tripRepository.activeKind.value == TripSourceKind.SIMULATED) snap.simRemainSec?.let(c.simTrip::jumpToRemain)
            val session = GameSession(
                speech = speechPort,
                clock = c.clock,
                content = content,
                dictionary = dictionary,
                players = snap.players,
                initialPlan = snap.plan,
                choice = snap.choice,
                demo = snap.demo,
                drowsy = monitor,
                inputs = inputs,
                callbacks = callbacks,
                destinationName = snap.destinationName,
                presetId = snap.presetId,
                restore = snap,
            )
            // 복원된 세션은 자동 재생하지 않는다 — 사용자가 "계속"을 눌러야 한다 (DEV_GUIDE §7.3)
            mode.addPause(PauseReason.USER)
            AppLog.i(TAG, "session restored at round ${snap.roundIndex + 1}")
            launchSession(session, keepPauses = true)
        }
    }

    fun discardSaved() {
        _restorable.value = null
        scope.launch { c.preferences.saveSession(null) }
    }

    private fun launchSession(session: GameSession, keepPauses: Boolean = false) {
        _summary.value = null
        _stage1CardVisible.value = false
        restCare.clear()
        resetTbtPause()
        screenQuizzes.forEach { it.reset() }
        if (!keepPauses) mode.clearPauses()
        _session.value = session
        mode.sessionActive.value = true
        sessionJob = scope.launch {
            try {
                session.run()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // 어떤 경로에서도 크래시 금지: 세션을 정리하고 요약으로 보낸다
                AppLog.e(TAG, "session crashed — ending gracefully", t)
                finishSession(session.buildSummary())
            }
        }
    }

    private val callbacks = object : SessionCallbacks {
        override fun onVoiceRoundActive(active: Boolean) {
            mode.voiceRoundActive.value = active
            if (!active) c.audioFocus.abandon()
        }

        override fun onStage1(driver: Player, newDifficultyCap: Int) {
            _stage1CardVisible.value = true
            stage1HideJob?.cancel()
            stage1HideJob = scope.launch {
                delay(STAGE1_CARD_MS)
                _stage1CardVisible.value = false
            }
        }

        override fun suggestRestStop(): RestStopSuggestion? = restCare.suggest()

        override fun headingRestStop(): RestStopSuggestion? = restCare.headingTo

        override fun onStage2(driver: Player, suggestion: RestStopSuggestion?) {
            stage1HideJob?.cancel()
            _stage1CardVisible.value = false
            restCare.offer(suggestion)
        }

        override fun onRestDecision(suggestion: RestStopSuggestion?, accepted: Boolean) = restCare.decide(suggestion, accepted)

        override fun onRestHold() = restCare.holdAtRestArea()

        override fun onSnapshot(snapshot: SessionSnapshot) = persist(snapshot)

        override fun onFinished(summary: SessionSummary) {
            finishSession(summary)
        }

        override fun log(message: String) = AppLog.i("GameSession", message)

        override fun logSensitive(label: String, value: String) = AppLog.sensitive("GameSession", label, value)
    }

    /** 세션이 끝남(도착·사용자 종료·오류): 요약을 보이고 기준값을 남기고 저장본을 지운다 */
    private fun finishSession(summary: SessionSummary, showSummary: Boolean = true) {
        val s = _session.value
        _summary.value = if (showSummary) summary else null
        // 데모 모드는 시연용 기준값으로 시작하므로 운전자 기준값으로 남기지 않는다
        s?.takeIf { !it.demo }?.drowsy?.currentBaseline?.let { b -> scope.launch { c.preferences.saveBaseline(activeDriverId, b) } }
        scope.launch { c.preferences.saveSession(null) }
        tearDownSession()
        AppLog.i(TAG, if (showSummary) "session ended → summary" else "session ended → home")
    }

    /** 세션의 메모리 상태와 그 세션이 잡은 것(여정·화면 게임 읽기·오디오 포커스·쉬어 가기·TBT 일시정지)을 정리한다 */
    private fun tearDownSession() {
        sessionJob?.cancel()
        sessionJob = null
        _session.value = null
        mode.voiceRoundActive.value = false
        mode.sessionActive.value = false
        mode.clearPauses()
        c.tripRepository.endTrip()
        stopScreenQuizzes("session end")
        // 통화 중(일시 손실)에 끝내도 요청을 거둔다 — 남겨 두면 통화 뒤 포커스가 돌아와 다른 미디어가 계속 작게 들린다
        c.audioFocus.abandon(force = true)
        _stage1CardVisible.value = false
        restCare.clear()
        resetTbtPause()
        // 세션 중에는 내비 상태 변화를 홈에 반영하지 않았다 → 끝난 지금 맞춘다 (길 안내가 끝났으면 프리셋으로; 새로 고르지는 않는다)
        _home.update { it.withNaviRoute(c.naviTrip.activeNaviDestination.value != null, autoSelect = false) }
    }

    /**
     * 사용자가 "게임 끝내기"를 누름 (TTS 없이 바로 정리). 진행 중인 발화·인식은 즉시 멈춘다.
     * 한 문제라도 했으면 지금까지의 점수를 요약 화면으로 보여 주고("처음으로" → 홈), 아니면 바로 홈으로 간다.
     * 내비의 길 안내는 취소하지 않는다.
     */
    fun endSession() {
        val s = _session.value ?: return
        sessionJob?.cancel()
        c.speechRouter.stopAll()
        val summary = s.buildSummary().copy(endedByUser = true)
        val played = summary.roundsPlayed.isNotEmpty() || summary.scores.values.any { it > 0 } ||
            summary.correctCounts.values.any { it > 0 }
        AppLog.i(TAG, "user ended the game (played=$played)")
        finishSession(summary, showSummary = played)
    }

    fun dismissSummary() {
        _summary.value = null
    }

    // ── 일시정지 / 계속 / 다음 판 ──────────────────────────────

    fun pause() = mode.addPause(PauseReason.USER)

    fun resume() {
        listOf(PauseReason.USER, PauseReason.BACKGROUND, PauseReason.AUDIO_UNAVAILABLE, PauseReason.REST_STOP, PauseReason.REST_AREA)
            .forEach(mode::removePause)
        _session.value?.releaseRestHold()
        c.speechRouter.markNewStream()
    }

    fun startNextRoundNow() = _session.value?.requestNextRoundNow()

    // ── 졸음 케어 2단계 (쉬어 가기) ────────────────────────────

    /** 화면 "쉬어 가기": 음성 질문을 기다리는 중이면 그 답으로, 아니면 바로 쉬어 가기 시작 */
    fun acceptRest() {
        if (_session.value?.answerRestSuggestion(true) != true) restCare.acceptOffered()
    }

    fun declineRest() {
        if (_session.value?.answerRestSuggestion(false) != true) restCare.dismiss()
    }

    fun cancelRest() = restCare.cancel()

    /** 데모 모드는 쿨다운만 짧게 (3분 시연 안에 1단계 → 2단계) — 판단 규칙은 같다 */
    private fun drowsyConfig(demo: Boolean) = if (demo) DrowsinessMonitor.Config.DEMO else DrowsinessMonitor.Config()

    // ── 오디오 포커스 (DEV_GUIDE §7.2) ─────────────────────────

    private fun ensureFocus(): Boolean {
        if (c.audioFocus.hasFocus.value) return true
        val granted = c.audioFocus.request()
        if (!granted) {
            AppLog.w(TAG, "audio focus denied → PAUSED(AUDIO_UNAVAILABLE)")
            mode.addPause(PauseReason.AUDIO_UNAVAILABLE)
        }
        return granted
    }

    private fun observeFocus() {
        c.audioFocus.changes.onEach { change ->
            val ownTts = c.speechRouter.ttsKind.value == SpeechEngineKind.PLEOS && c.speechRouter.pleosSpeaking.value
            // 정차 중 화면 게임이 읽는 중: 그 읽기만 멈춘다 (음성 판이 없으니 세션을 일시정지·종료하지 않는다)
            val reading = screenQuizzes.firstOrNull { it.holdsFocus }
            if (reading != null) {
                reading.onFocusChange(change, ownTts)
                return@onEach
            }
            val s = _session.value ?: return@onEach
            when (SessionFocusPolicy.decide(change, ownTts, PauseReason.AUDIO_FOCUS in mode.pauseReasons.value)) {
                SessionFocusPolicy.Action.IGNORE_SELF_INDUCED -> {
                    // Pleos TTS 는 Speech Service 경유로 자체 포커스를 잡는다 → 우리 발화로 인한 손실은 무시
                    AppLog.i(TAG, "${change.name} during own Pleos TTS → self-induced, ignore")
                    c.audioFocus.markLost()
                }
                SessionFocusPolicy.Action.PAUSE_UNTIL_GAIN -> {
                    AppLog.i(TAG, "LOSS_TRANSIENT (통화·Gleo) → 즉시 중단 + PAUSED")
                    mode.addPause(PauseReason.AUDIO_FOCUS)
                }
                SessionFocusPolicy.Action.PAUSE_UNTIL_USER -> {
                    // 영구 손실 뒤에는 GAIN 이 오지 않는다 → "계속"으로 풀리는 사유로 멈추고 요청을 거둔다 (자동 재개 없음)
                    AppLog.w(TAG, "AUDIOFOCUS_LOSS → 일시정지(상태 저장), 계속을 누르면 이어서")
                    mode.replacePause(PauseReason.AUDIO_FOCUS, PauseReason.AUDIO_UNAVAILABLE)
                    c.audioFocus.abandon(force = true)
                    saveSnapshot(s)
                }
                SessionFocusPolicy.Action.RESUME -> {
                    AppLog.i(TAG, "GAIN → 1초 뒤 같은 문제 다시")
                    c.speechRouter.markNewStream()
                    mode.removePause(PauseReason.AUDIO_FOCUS)
                }
                SessionFocusPolicy.Action.CONTINUE ->
                    if (change == FocusChange.LOSS_TRANSIENT_CAN_DUCK) AppLog.i(TAG, "CAN_DUCK → 계속 (게임은 발화자라 duck 대상 아님)")
            }
        }.launchIn(scope)
    }

    /**
     * 통화 일시정지(AUDIO_FOCUS)는 GAIN 으로만 풀린다. 그런데 포커스 요청이 사라지면(정차 중 화면 게임이 같은 요청을
     * 거둠 등) GAIN 이 오지 않아 "계속" 없이 멈춘 채 남는다 → 그때는 "계속"으로 풀리는 사유로 바꾼다.
     */
    private fun observeFocusRequest() {
        c.audioFocus.requestHeld.onEach { held ->
            if (held || _session.value == null || PauseReason.AUDIO_FOCUS !in mode.pauseReasons.value) return@onEach
            AppLog.i(TAG, "focus request dropped during AUDIO_FOCUS pause → 계속을 누르면 이어서")
            mode.replacePause(PauseReason.AUDIO_FOCUS, PauseReason.AUDIO_UNAVAILABLE)
        }.launchIn(scope)
    }

    /** PAUSED 진입 시 포커스 반납 (통화 대기 중에는 GAIN 을 받기 위해 유지) */
    private fun observePauseFocusPolicy() {
        combine(gameMode, mode.pauseReasons) { m, reasons -> m to reasons }
            .distinctUntilChanged()
            .onEach { (m, reasons) ->
                if (m == GameMode.PAUSED && PauseReason.AUDIO_FOCUS !in reasons) c.audioFocus.abandon()
                if (m == GameMode.PAUSED) c.speechRouter.markNewStream()
            }.launchIn(scope)
    }

    // ── 여정 ────────────────────────────────────────────────────

    private fun observeTrip() {
        c.tripRepository.snapshot.onEach { snap ->
            val s = _session.value ?: return@onEach
            val remain = snap?.remainTimeSec ?: return@onEach
            if (remain > 0) s.onTripUpdate(remain, snap.timeScale)
        }.launchIn(scope)
        c.tripRepository.events.onEach { e ->
            when (e) {
                is TripEvent.Arrived -> if (_session.value != null) {
                    AppLog.i(TAG, "destination arrived → 결과 정리")
                    arrived.value = true
                }
                is TripEvent.Error -> AppLog.w(TAG, "trip error ${e.reason}")
                TripEvent.Cancelled -> AppLog.i(TAG, "route cancelled in navi — 게임은 계속")
                is TripEvent.RouteStarted -> Unit
            }
        }.launchIn(scope)
    }

    /** 내비 앱에서 길 안내를 시작·종료하면 홈의 목적지 선택에 바로 반영한다 (게임 중에는 건드리지 않음) */
    private fun observeNaviRoute() {
        c.naviTrip.activeNaviDestination.map { it != null }.distinctUntilChanged().onEach { available ->
            if (_session.value != null) return@onEach
            val before = _home.value.useNaviRoute
            val autoSelect = _settings.value.tripMode != SourceMode.SIM
            _home.update { it.withNaviRoute(available, autoSelect) }
            if (before != _home.value.useNaviRoute) {
                AppLog.i(TAG, if (available) "navi route detected → 홈에서 내비 목적지 선택" else "navi route ended → 프리셋으로")
            }
        }.launchIn(scope)
    }

    /**
     * P1: 분기/진출입/회전 안내가 500 m 이내면 PAUSED, 지나면(3초 유지) 재개.
     * 같은 안내가 [TBT_STALE_MS] 넘게 그대로면(정체·정차, 또는 움직이지 않는 에뮬레이터) 그 안내로는 더 멈추지 않는다.
     */
    private fun observeTbt() {
        c.tripRepository.snapshot.map { it?.nextTurn }.distinctUntilChanged().onEach { turn ->
            if (_session.value == null) return@onEach
            val key = turn?.let { "${it.typeName}|${it.description}" }
            val attention = turn != null && turn.needsAttention && turn.distanceM <= TBT_PAUSE_DISTANCE_M && key != tbtSuppressedKey
            val paused = PauseReason.NAV_GUIDANCE in mode.pauseReasons.value
            if (key != tbtSuppressedKey) tbtSuppressedKey = null
            if (attention) {
                tbtClearJob?.cancel()
                if (!paused || key != tbtActiveKey) {
                    AppLog.i(TAG, "TBT ${turn?.typeName} ${turn?.distanceM}m → 자동 일시정지")
                    mode.addPause(PauseReason.NAV_GUIDANCE)
                    tbtActiveKey = key
                    tbtStaleJob?.cancel()
                    tbtStaleJob = scope.launch {
                        delay(TBT_STALE_MS)
                        if (PauseReason.NAV_GUIDANCE in mode.pauseReasons.value && tbtActiveKey == key) {
                            AppLog.i(TAG, "TBT 안내가 ${TBT_STALE_MS / 1000}초 넘게 그대로 → 정체/정차로 보고 재개")
                            tbtSuppressedKey = key
                            mode.removePause(PauseReason.NAV_GUIDANCE)
                        }
                    }
                }
            } else if (paused && tbtClearJob?.isActive != true) {
                tbtStaleJob?.cancel()
                tbtClearJob = scope.launch {
                    delay(TBT_RESUME_HOLD_MS)
                    AppLog.i(TAG, "TBT 구간 통과 → 재개")
                    tbtActiveKey = null
                    mode.removePause(PauseReason.NAV_GUIDANCE)
                }
            }
        }.launchIn(scope)
    }

    private var tbtStaleJob: Job? = null
    private var tbtActiveKey: String? = null
    private var tbtSuppressedKey: String? = null

    /** 세션 시작·끝: 지난 세션의 TBT 판단(같은 안내 억제 키·타이머)이 다음 세션의 첫 분기점에 남지 않게 한다 */
    private fun resetTbtPause() {
        tbtClearJob?.cancel()
        tbtStaleJob?.cancel()
        tbtActiveKey = null
        tbtSuppressedKey = null
    }

    // ── 생명주기 ────────────────────────────────────────────────

    fun onBackground() {
        stopScreenQuizzes("background")
        val s = _session.value ?: return
        val phase = sessionState.value?.phase
        backgroundInterrupted = mode.voiceRoundActive.value ||
            phase in setOf(SessionPhase.INTRO, SessionPhase.ASKING, SessionPhase.LISTENING, SessionPhase.FEEDBACK, SessionPhase.ROUND_END)
        AppLog.i(TAG, "background → 중단·포커스 반납·상태 저장 (interrupted=$backgroundInterrupted)")
        mode.addPause(PauseReason.BACKGROUND)
        c.speechRouter.stopAll()
        c.audioFocus.abandon()
        saveSnapshot(s)
    }

    /** 중단(백그라운드·포커스 영구 손실)에 대비해 지금 상태를 저장한다 — 이어서 하기의 원본 */
    private fun saveSnapshot(s: GameSession) = persist(s.snapshot())

    private fun persist(snapshot: SessionSnapshot) {
        val full = fullSnapshot(snapshot)
        scope.launch { c.preferences.saveSession(full) }
    }

    /** 세션이 모르는 앱 쪽 정보(운전자 프로필 id, 시뮬 여정의 남은 시간)를 붙인다 */
    private fun fullSnapshot(snapshot: SessionSnapshot): SessionSnapshot {
        val simRemain = if (c.tripRepository.activeKind.value == TripSourceKind.SIMULATED) c.tripRepository.snapshot.value?.remainTimeSec else null
        return snapshot.copy(driverProfileId = activeDriverId, simRemainSec = simRemain)
    }

    fun onForeground() {
        if (PauseReason.BACKGROUND in mode.pauseReasons.value && !backgroundInterrupted) {
            // 진행 중이던 발화가 없었다면(쉬는 시간 등) 그대로 이어 간다. 끊긴 발화는 사용자가 "계속"을 눌러야 재개.
            mode.removePause(PauseReason.BACKGROUND)
        }
    }

    /**
     * 액티비티가 실제로 끝날 때(작업 제거). 진행 중이던 세션은 메모리에서만 정리하고 저장본은 남긴다 —
     * 같은 프로세스에서 다시 열면 "이어서 하기"로 고를 수 있다 (프로세스가 죽었으면 다음 실행 때 DataStore 에서 읽는다).
     */
    fun shutdown() {
        val s = _session.value ?: return
        _restorable.value = fullSnapshot(s.snapshot())
        tearDownSession()
        AppLog.i(TAG, "shutdown → session kept as restorable")
    }

    // ── 디버그 패널 ─────────────────────────────────────────────

    fun setDemoMode(on: Boolean) = updateSettings { it.copy(demoMode = on) }
    fun setCarMode(m: SourceMode) = updateSettings { it.copy(carMode = m) }
    fun setTripMode(m: SourceMode) = updateSettings { it.copy(tripMode = m) }
    fun setLocationMode(m: SourceMode) = updateSettings { it.copy(locationMode = m) }
    fun setSpeechMode(m: SpeechMode) = updateSettings { it.copy(speechMode = m) }
    fun setAnswerInput(input: AnswerInput) = updateSettings { it.copy(answerInput = input) }
    fun setSimTimeScale(scale: Float) = updateSettings { it.copy(simTimeScale = scale) }

    /** 슬라이더를 끄는 동안에는 바로 반영만 한다 — 저장은 손을 뗄 때 [setSimTimeScale] */
    fun previewSimTimeScale(scale: Float) {
        c.simTrip.timeScale.value = scale
    }

    private fun setAnswerInputInternal(input: AnswerInput) {
        c.speechRouter.answerInput.value = input
        c.scriptedSpeech.setScript(if (input == AnswerInput.MANUAL || input == AnswerInput.MIC) null else c.content.loadScript(input))
    }

    fun simGear(gear: GearPosition) = c.simCar.setGear(gear)
    fun simSpeed(mps: Float) = c.simCar.setSpeed(mps)
    fun simDo(required: Boolean?) = c.simCar.setDistractionOptimization(required)
    fun simRegion(code: String?) {
        c.locationRepository.manualRegion.value = code
    }
    fun simJumpRemain(sec: Int) = c.simTrip.jumpToRemain(sec)
    fun simTriggerTbt() = c.simTrip.triggerTbt()
    fun simStage2() = _session.value?.requestStage2()
    fun simRestArrival() = restCare.arrive("디버그 패널")
    fun manualAnswer(answer: ScriptedSpeechEngine.ManualAnswer) = c.scriptedSpeech.submitManual(answer)

    /** 디버그 "운전자 기준값 초기화". 세션 중이면 지금 세션도 다음 응답부터 다시 기준을 만든다 (끝날 때 옛 값을 다시 저장하지 않게) */
    fun resetBaseline() {
        val s = _session.value
        val driverId = if (s != null) activeDriverId else _home.value.driverId
        s?.drowsy?.resetBaseline()
        scope.launch { c.preferences.saveBaseline(driverId, null) }
        AppLog.i(TAG, "driver baseline reset ($driverId${if (s != null) ", 세션 중 → 다시 캘리브레이션" else ""})")
    }

    companion object {
        private const val TAG = "Coordinator"
        private const val STAGE1_CARD_MS = 15_000L
        private const val TBT_PAUSE_DISTANCE_M = 500
        private const val TBT_RESUME_HOLD_MS = 3_000L
        private const val TBT_STALE_MS = 20_000L
        private const val RESTORE_MAX_AGE_MS = 12 * 60 * 60 * 1000L

        /** 데모 모드에서 기준값이 없을 때 넣는 평소 응답 시간 */
        val DEMO_BASELINE = DriverBaseline(latencyEmaMs = 1_500.0, accuracyEma = 0.9, sampleCount = 0)
    }
}
