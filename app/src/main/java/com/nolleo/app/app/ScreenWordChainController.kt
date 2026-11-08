package com.nolleo.app.app

import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.speech.FocusChange
import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.engine.SpeechPort
import com.nolleo.app.domain.engine.WordChainPlay
import com.nolleo.app.domain.model.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 정차 중 끝말잇기 — 주행 중 음성 판과 같은 규칙으로 놀레오·운전자·동승자가 **말로** 잇는다
 * (11/8 사용자 요청: 넷 중에 고르기 대신 진짜 끝말잇기). 진행 규칙은 [WordChainPlay], 여기서는 소리·포커스·멈춤만 맡는다.
 *
 *  - 카드를 누르면 시작하고, 닫았다 다시 열면 멈춘 차례부터 잇는다(이어진 낱말은 그대로). 끝난 판이면 새 판.
 *  - 오디오 (DEV_GUIDE §7.2–7.3): 판이 도는 동안(말하기·듣기) AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 을 들고 있다가
 *    끝나거나 멈추면 반납한다. 출발·닫기·백그라운드·세션 종료·음성 게임의 발화·포커스 손실이면 즉시 멈추고,
 *    저절로 이어 하지 않는다 ("이어서 하기"를 누른다).
 *  - 판의 기록은 세션 동안 남고 새 세션에서 처음부터.
 */
class ScreenWordChainController(
    private val c: AppContainer,
    private val scope: CoroutineScope,
    /** 지금 세션의 참가자 (운전자 + 동승자) */
    private val players: () -> List<Player>,
) : ScreenGameController {

    data class UiState(
        val play: WordChainPlay.State = WordChainPlay.State(),
        /** 판이 도는 중 (말하거나 듣는 중) */
        val running: Boolean = false,
        /** 시작했다가 멈춤(닫기·출발·포커스 등) — "이어서 하기"로 같은 차례부터 */
        val paused: Boolean = false,
        /** 포커스를 얻지 못했거나 잃어서 멈춤 */
        val voiceStopped: Boolean = false,
    )

    override val kind = ScreenGameKind.WORD

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var play: WordChainPlay? = null
    private var runJob: Job? = null
    private var mirrorJob: Job? = null
    /** 멈춘 판의 늦은 정리가 새 판의 상태를 덮어쓰지 않게 하는 세대 번호 */
    private var generation = 0

    override var holdsFocus: Boolean = false
        private set

    override val isAvailable: Boolean get() = c.content.dictionary.value != null

    private val speech = object : SpeechPort {
        override suspend fun speak(text: String) {
            if (!ensureFocus()) {
                // 포커스를 못 얻으면 말하지 않고 멈춘다 (저절로 다시 시도하지 않음)
                halt(voiceStopped = true)
                throw CancellationException("audio focus denied")
            }
            c.speechRouter.speak(text)
        }

        override fun listen(hint: ListenHint): Flow<SttEvent> = c.speechRouter.listen(hint)

        override fun stop() = c.speechRouter.stopAll()

        override fun markNewStream() = c.speechRouter.markNewStream()
    }

    // ── 사용자 동작 ──────────────────────────────────────────────

    /** 카드를 눌러 열기·"이어서 하기": 멈춘 판이면 같은 차례부터, 끝났거나 없으면 새 판 */
    override fun open() {
        val p = play?.takeIf { it.state.value.ended == null } ?: newPlay() ?: return
        AppLog.i(TAG, "open (words=${p.state.value.words.size})")
        start(p)
    }

    /** "새로 시작"·"다시 하기": 지금 판을 버리고 새 판 */
    fun restart() {
        stop("restart")
        val p = newPlay() ?: return
        AppLog.i(TAG, "restart")
        start(p)
    }

    override fun close() = stop("close")

    // ── 코디네이터가 부르는 정리 ────────────────────────────────

    override fun stop(reason: String) {
        val active = runJob?.isActive == true
        if (!active && !holdsFocus) return
        AppLog.i(TAG, "stop ($reason)")
        halt(voiceStopped = false)
    }

    override fun reset() {
        stop("reset")
        mirrorJob?.cancel()
        mirrorJob = null
        play = null
        _state.value = UiState()
    }

    override fun onFocusChange(change: FocusChange, ownTts: Boolean) {
        when (change) {
            FocusChange.LOSS_TRANSIENT, FocusChange.LOSS -> {
                if (ownTts) {
                    // Pleos TTS 는 Speech Service 가 따로 포커스를 잡는다 → 우리 발화로 생긴 손실은 무시
                    c.audioFocus.markLost()
                    return
                }
                AppLog.i(TAG, "focus ${change.name} (통화·Gleo 등) → 끝말잇기 즉시 멈춤, 자동 재개 없음")
                halt(voiceStopped = true)
            }
            FocusChange.GAIN, FocusChange.LOSS_TRANSIENT_CAN_DUCK -> Unit
        }
    }

    // ── 내부 ──────────────────────────────────────────────────

    private fun newPlay(): WordChainPlay? {
        val dictionary = c.content.dictionary.value ?: return null
        val p = WordChainPlay(
            dictionary = dictionary,
            players = players(),
            speech = speech,
            clock = c.clock,
            log = { AppLog.i(TAG, it) },
            logSensitive = { label, value -> AppLog.sensitive(TAG, label, value) },
        )
        play = p
        mirrorJob?.cancel()
        mirrorJob = scope.launch { p.state.collect { s -> _state.update { it.copy(play = s) } } }
        _state.update { it.copy(play = p.state.value, paused = false, voiceStopped = false) }
        return p
    }

    private fun start(p: WordChainPlay) {
        if (runJob?.isActive == true) return
        val gen = ++generation
        _state.update { it.copy(running = true, paused = false, voiceStopped = false) }
        runJob = scope.launch {
            p.run()
            // 끝까지 갔다 (놀레오가 졌거나 대답이 없어 멈춤) — 멈춘 판의 늦은 완료는 무시
            if (gen != generation) return@launch
            runJob = null
            releaseFocus()
            _state.update { it.copy(running = false) }
        }
    }

    /** 말하기·듣기를 그 자리에서 끊고 포커스를 반납한다. 진행은 남겨 "이어서 하기"로 잇는다. */
    private fun halt(voiceStopped: Boolean) {
        generation++
        val job = runJob
        runJob = null
        val wasActive = job?.isActive == true
        job?.cancel()
        if (wasActive) c.speechRouter.stopAll()
        releaseFocus()
        _state.update {
            val started = it.play.words.isNotEmpty() || it.play.player != null
            it.copy(
                running = false,
                paused = it.play.ended == null && started,
                voiceStopped = it.voiceStopped || voiceStopped,
            )
        }
    }

    private fun ensureFocus(): Boolean {
        if (c.audioFocus.hasFocus.value) {
            holdsFocus = true
            return true
        }
        val granted = c.audioFocus.request()
        if (granted) {
            // 처음 잡을 때만 새 스트림(선행 묵음) — 자기 Pleos TTS 때문에 잠깐 잃었다 다시 잡을 때는 아니다
            if (!holdsFocus) c.speechRouter.markNewStream()
            holdsFocus = true
        } else {
            AppLog.w(TAG, "audio focus denied → 끝말잇기 멈춤 (자동 재개 없음)")
        }
        return granted
    }

    private fun releaseFocus() {
        if (!holdsFocus) return
        holdsFocus = false
        c.audioFocus.abandon(force = true)
    }

    private companion object {
        const val TAG = "WordChain"
    }
}
