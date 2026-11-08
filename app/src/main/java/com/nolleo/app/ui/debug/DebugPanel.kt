package com.nolleo.app.ui.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.BuildConfig
import com.nolleo.app.app.AppContainer
import com.nolleo.app.app.RestCareController
import com.nolleo.app.core.SourceMode
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.navi.DestinationPreset
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.core.speech.AnswerInput
import com.nolleo.app.core.speech.ScriptedSpeechEngine
import com.nolleo.app.core.speech.SpeechMode
import com.nolleo.app.domain.model.GearPosition
import com.nolleo.app.domain.rest.RestStopFinder
import com.nolleo.app.ui.theme.Nl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 디버그 패널 (제목 롱프레스로 열기, release 에서도 접근 가능하되 상단에 DEV 표시).
 * 선택된 소스/엔진 상태, 시뮬레이션 토글, 대본 선택, 졸음 지표, 최근 로그 20줄.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DebugPanel(container: AppContainer, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val coordinator = container.coordinator
    val settings by coordinator.settings.collectAsStateWithLifecycle()
    val carKind by container.carRepository.activeKind.collectAsStateWithLifecycle()
    val aaosStatus by container.aaosCar.status.collectAsStateWithLifecycle()
    val signals by container.carRepository.signals.collectAsStateWithLifecycle()
    val ux by container.carRepository.ux.collectAsStateWithLifecycle()
    val uxReal by container.carRepository.uxIsReal.collectAsStateWithLifecycle()
    val vehicleStatus by container.vehicleSdk.status.collectAsStateWithLifecycle()
    val brakeStatus by container.vehicleSdk.brakeStatus.collectAsStateWithLifecycle()
    val seatStatus by container.vehicleSdk.seatStatus.collectAsStateWithLifecycle()
    val seats by container.vehicleSdk.seatOccupancy.collectAsStateWithLifecycle()
    val decision by container.modeController.decision.collectAsStateWithLifecycle()
    val reasons by container.modeController.pauseReasons.collectAsStateWithLifecycle()
    val tripKind by container.tripRepository.activeKind.collectAsStateWithLifecycle()
    val naviStatus by container.naviTrip.status.collectAsStateWithLifecycle()
    val naviEvent by container.naviTrip.lastEventLabel.collectAsStateWithLifecycle()
    val fallback by container.tripRepository.fallbackReason.collectAsStateWithLifecycle()
    val trip by container.tripRepository.snapshot.collectAsStateWithLifecycle()
    val scale by container.simTrip.timeScale.collectAsStateWithLifecycle()
    val fusedStatus by container.fusedLocation.status.collectAsStateWithLifecycle()
    val reading by container.locationRepository.reading.collectAsStateWithLifecycle()
    val manualRegion by container.locationRepository.manualRegion.collectAsStateWithLifecycle()
    val ttsKind by container.speechRouter.ttsKind.collectAsStateWithLifecycle()
    val sttKind by container.speechRouter.sttKind.collectAsStateWithLifecycle()
    val pleosTts by container.pleosSpeech.ttsStatus.collectAsStateWithLifecycle()
    val pleosStt by container.pleosSpeech.sttStatus.collectAsStateWithLifecycle()
    val androidTts by container.androidSpeech.ttsStatus.collectAsStateWithLifecycle()
    val androidStt by container.androidSpeech.sttStatus.collectAsStateWithLifecycle()
    val focus by container.audioFocus.hasFocus.collectAsStateWithLifecycle()
    val scriptedListening by container.scriptedSpeech.listening.collectAsStateWithLifecycle()
    val session by coordinator.session.collectAsStateWithLifecycle()
    val sessionState by coordinator.sessionState.collectAsStateWithLifecycle()
    val logs by AppLog.recent.collectAsStateWithLifecycle()
    val content by container.content.content.collectAsStateWithLifecycle()
    val restAreas by container.content.restAreas.collectAsStateWithLifecycle()
    val serviceAreas by container.content.serviceAreas.collectAsStateWithLifecycle()
    val rest by coordinator.restCare.state.collectAsStateWithLifecycle()
    val restDistance by coordinator.restCare.distanceM.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(Nl.colors.ground.copy(alpha = 0.97f)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DEV", style = MaterialTheme.typography.titleLarge, color = Nl.colors.onAction,
                modifier = Modifier.background(Nl.colors.action, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp))
            Text("  디버그 패널 · v${BuildConfig.VERSION_NAME}${if (BuildConfig.DEBUG) " debug" else " release"}",
                style = MaterialTheme.typography.titleLarge, color = Nl.colors.ink, modifier = Modifier.weight(1f))
            DevButton("닫기", onClick = onClose)
        }

        Section("모드") {
            Line("모드 ${decision.mode.label} · 주행 ${decision.driveState.label} (${decision.reason}) · 정지사유 ${reasons.joinToString { it.name }.ifEmpty { "-" }}")
            Line("규제 DO=${ux.requiresDO} bits=${ux.activeRestrictions} (${if (uxReal) "실제 CarUxRestrictions" else "시뮬"}) · 포커스 ${if (focus) "보유" else "없음"}")
            Toggle("데모 모드(판당 ${com.nolleo.app.domain.engine.SessionPlanner.DEMO_QUESTION_COUNT}문제·기준값 시드)", settings.demoMode) { coordinator.setDemoMode(it) }
        }

        Section("차량 — ${carKind.label}") {
            Line("AAOS Car API: ${aaosStatus.label} · gear=${signals.gear} speed=${signals.speedMps?.let { "%.1f".format(it) } ?: "-"}m/s pbrake=${signals.parkingBrakeOn} brake=${signals.brakePedalPressed}")
            Line("Pleos Vehicle: ${vehicleStatus.label} · Brake ${brakeStatus.label} · Seat ${seatStatus.label} ${seats.entries.joinToString { "${it.key}=${if (it.value) "착좌" else "빈"}" }}")
            ModeRow(settings.carMode) { coordinator.setCarMode(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevButton("시뮬 기어 P") { coordinator.simGear(GearPosition.PARK) }
                DevButton("시뮬 기어 D") { coordinator.simGear(GearPosition.DRIVE) }
                DevButton("속도 0") { coordinator.simSpeed(0f) }
                DevButton("속도 30km/h") { coordinator.simSpeed(8.3f) }
                DevButton("속도 100km/h") { coordinator.simSpeed(27.8f) }
                DevButton("시뮬 DO 켬") { coordinator.simDo(true) }
                DevButton("시뮬 DO 끔") { coordinator.simDo(false) }
                DevButton("시뮬 DO 자동") { coordinator.simDo(null) }
            }
        }

        Section("여정 — ${tripKind.label}${fallback?.let { " (폴백: $it)" } ?: ""}") {
            Line("NaviHelper: ${naviStatus.label} · 마지막 이벤트 ${naviEvent ?: "-"}")
            Line("남은 ${trip?.remainTimeSec?.let(TripUnits::formatDuration) ?: "-"} · ${trip?.remainDistanceM?.let(TripUnits::formatDistance) ?: "-"} · TBT ${trip?.nextTurn?.let { "${it.typeName} ${it.distanceM}m" } ?: "-"}")
            ModeRow(settings.tripMode) { coordinator.setTripMode(it) }
            Line("시뮬 경로 (여정=시뮬일 때 홈의 현재 경로 — 시연용)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinationPreset.ALL.forEach { p ->
                    DevButton(p.title + if (settings.simPresetId == p.id) " ✓" else "") { coordinator.setSimPreset(p.id) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevButton("남은 5분으로") { coordinator.simJumpRemain(5 * 60) }
                DevButton("남은 30분으로") { coordinator.simJumpRemain(30 * 60) }
                DevButton("TBT 분기점 300m") { coordinator.simTriggerTbt() }
            }
            Line("시뮬 배율: 실제 1분 = 여정 ${scale.toInt()}분")
            Slider(
                value = scale,
                onValueChange = coordinator::previewSimTimeScale,
                onValueChangeFinished = { coordinator.setSimTimeScale(container.simTrip.timeScale.value) },
                valueRange = 1f..60f,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Section("위치 — ${reading.source.label}: ${content?.region(reading.regionCode)?.name ?: "-"}") {
            Line("FusedLocation: ${fusedStatus.label}")
            ModeRow(settings.locationMode) { coordinator.setLocationMode(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevButton(if (manualRegion == null) "수동 지정 없음 ✓" else "수동 지정 해제") { coordinator.simRegion(null) }
                content?.regions?.forEach { r -> DevButton(r.shortName + if (manualRegion == r.code) " ✓" else "") { coordinator.simRegion(r.code) } }
            }
        }

        Section("음성 — TTS ${ttsKind.label} · STT ${sttKind.label}") {
            Line("Pleos(${container.pleosSpeech.modeLabel}) TTS ${pleosTts.label} / STT ${pleosStt.label} · Android TTS ${androidTts.label} / STT ${androidStt.label}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeechMode.entries.forEach { m -> DevButton(m.label + if (settings.speechMode == m) " ✓" else "") { coordinator.setSpeechMode(m) } }
            }
            Line("응답 입력")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerInput.entries.forEach { a -> DevButton(a.label + if (settings.answerInput == a) " ✓" else "") { coordinator.setAnswerInput(a) } }
            }
            Line("직접 응답 (대본 엔진이 듣는 중일 때) ${if (scriptedListening) "· 듣는 중" else ""}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevButton("정답") { coordinator.manualAnswer(ScriptedSpeechEngine.ManualAnswer.Correct) }
                DevButton("오답") { coordinator.manualAnswer(ScriptedSpeechEngine.ManualAnswer.Wrong) }
                DevButton("무응답") { coordinator.manualAnswer(ScriptedSpeechEngine.ManualAnswer.Silence) }
            }
            // 주행 규제(requiresDO) 중에는 텍스트 입력 금지 (DEV_GUIDE §7.1 NO_KEYBOARD)
            var typed by remember { mutableStateOf("") }
            val keyboardAllowed = !ux.requiresDO && !ux.noKeyboard
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    enabled = keyboardAllowed,
                    singleLine = true,
                    label = { Text(if (keyboardAllowed) "답 입력" else "주행 중 입력 불가") },
                    modifier = Modifier.weight(1f),
                )
                DevButton("보내기", enabled = keyboardAllowed && typed.isNotBlank()) {
                    coordinator.manualAnswer(ScriptedSpeechEngine.ManualAnswer.Text(typed))
                    typed = ""
                }
            }
        }

        Section("졸음 케어 (1단계 말 걸기 · 2단계 쉬어 가기)") {
            val d = session?.drowsy?.snapshot()
            if (d == null) {
                Line("세션 없음")
            } else {
                Line("기준값 ${d.baselineMs?.let { "${it}ms" } ?: "캘리브레이션 중(${d.calibrationCount}/5)"} · 정답률 EMA ${d.baselineAccuracy?.let { "%.2f".format(it) } ?: "-"}")
                Line("최근 5개 지연 ${d.recentLatencies.joinToString { "${it}ms" }.ifEmpty { "-" }} · 느림 ${d.slowCount}/5 · 난이도 상한 ${d.difficultyCap}")
                Line("Stage1 ${d.stage1Count}회 · 마지막 ${d.lastTriggerAtMs?.let { SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(it)) } ?: "-"} · Stage2 ${d.stage2Count}회")
            }
            Line("세션 단계 ${sessionState?.phase?.label ?: "-"} · 판 ${sessionState?.let { "${it.roundIndex + 1}/${it.plan.roundCount}" } ?: "-"}")
            // 2단계 미리보기 — 경로 위 목록(내비 TBT·시뮬 경로)과, 지금 걸리면 권할 곳
            val motion = remember(trip, reading) { coordinator.restCare.currentMotion() }
            val tablePick = remember(motion, restAreas) { motion?.let { (p, h) -> RestStopFinder.nearestAhead(restAreas, p, h) } }
            val pick = remember(trip, reading, restAreas, serviceAreas) { coordinator.restCare.preview() }
            Line(
                "졸음쉼터 자료 ${restAreas.size}곳 · 휴게소 정보 ${serviceAreas.size}곳 · 위치 ${if (motion == null) "모름(시뮬 여정 시작 또는 adb emu geo fix)" else "방향 ${motion.second.toInt()}°"}" +
                    " · 표의 앞쪽 졸음쉼터 ${tablePick?.let { (a, dm) -> "${a.displayName}(${a.to} 방향) ${TripUnits.formatDistance(dm)}" } ?: "-"}",
            )
            Line("경로 위: ${trip?.restStopsAhead?.let { l -> l.take(4).joinToString { "${it.name} ${TripUnits.formatDistance(it.distanceM)}" }.ifEmpty { "없음" } } ?: "정보 없음(길 안내 없음 → 표에서 고름)"}")
            Line("지금 걸리면 권할 곳: ${pick?.let { "${it.name} ${it.distanceLabel} (${it.source.label})${it.facilityLine?.let { f -> " · $f" } ?: ""}" } ?: "없음"}")
            Line("쉬어 가기 상태: ${restLabel(rest, restDistance)}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevButton("2단계 시험 (다음 운전자 답 뒤)", enabled = session != null) { coordinator.simStage2() }
                DevButton("쉼터 도착 (시뮬)", enabled = rest is RestCareController.State.Heading) { coordinator.simRestArrival() }
                DevButton("운전자 기준값 초기화") { coordinator.resetBaseline() }
                DevButton("여정 끝내기") { coordinator.endSession() }
            }
        }

        Section("최근 로그") {
            logs.takeLast(20).forEach { line ->
                Text(line, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp), color = Nl.colors.muted)
            }
        }
    }
}

private fun restLabel(state: RestCareController.State?, distanceM: Int?): String = when (state) {
    null -> "없음"
    is RestCareController.State.Offered -> "권유 중 · ${state.suggestion?.name ?: "고를 곳 없음"}"
    is RestCareController.State.Heading ->
        "가는 중 · ${state.suggestion.name} ${distanceM?.let(TripUnits::formatDistance) ?: ""} (${state.suggestion.source.label}) · 근접 알림 ${if (state.alertRegistered) "요청" else "없음 — 주차하면 도착"}" +
            (state.suggestion.area?.let { " · 도착 시험 geo fix ${it.longitude} ${it.latitude}" } ?: "")
    is RestCareController.State.Arrived -> "도착 (${state.trigger}) · 주차 후 다시 주행하면 끝"
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Nl.colors.card, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Nl.colors.ink)
        content()
    }
}

@Composable
private fun Line(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Nl.colors.ink)
}

@Composable
private fun DevButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Nl.colors.sunk, contentColor = Nl.colors.ink),
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    DevButton((if (value) "켜짐 ✓ " else "꺼짐 · ") + label) { onChange(!value) }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ModeRow(current: SourceMode, onSelect: (SourceMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SourceMode.entries.forEach { m -> DevButton(m.label + if (current == m) " ✓" else "") { onSelect(m) } }
    }
}
