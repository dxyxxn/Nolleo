package com.nolleo.app.app

import android.app.Application
import com.nolleo.app.BuildConfig
import com.nolleo.app.core.Clock
import com.nolleo.app.core.SystemClockSource
import com.nolleo.app.core.car.AaosCarStateSource
import com.nolleo.app.core.car.CarStateRepository
import com.nolleo.app.core.car.SimulatedCarStateSource
import com.nolleo.app.core.car.VehicleSdkClient
import com.nolleo.app.core.location.FusedLocationSource
import com.nolleo.app.core.location.LocationRepository
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.navi.NaviHelperTripSource
import com.nolleo.app.core.navi.SimulatedTripSource
import com.nolleo.app.core.navi.TripRepository
import com.nolleo.app.core.speech.AndroidSpeechEngine
import com.nolleo.app.core.speech.AudioFocusHelper
import com.nolleo.app.core.speech.PleosSpeechEngine
import com.nolleo.app.core.speech.ScriptedSpeechEngine
import com.nolleo.app.core.speech.SpeechRouter
import com.nolleo.app.data.AppPreferences
import com.nolleo.app.data.ContentRepository
import com.nolleo.app.domain.engine.ModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 수동 DI 컨테이너 (Hilt/kapt 미사용). SDK 싱글톤을 소유하고 앱 수명 동안 공유한다.
 * 입력 소스 3종(차량·여정·위치)과 음성 엔진은 런타임에 Real → Fallback → Simulated 순으로 자동 선택된다.
 */
class AppContainer(val application: Application) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val clock: Clock = SystemClockSource
    val preferences = AppPreferences(application)
    val content = ContentRepository(application)

    // ── 차량 상태 (AAOS Car API + Pleos Vehicle) ─────────────────
    val aaosCar = AaosCarStateSource(application)
    val vehicleSdk = VehicleSdkClient(application)
    val simCar = SimulatedCarStateSource()
    val carRepository = CarStateRepository(aaosCar, vehicleSdk, simCar, appScope)

    val modeController = ModeController(
        signals = carRepository.signals,
        ux = carRepository.ux,
        clock = clock,
        scope = appScope,
        log = { AppLog.i("ModeArbiter", it) },
    )

    // ── 여정 (NaviHelper) ───────────────────────────────────────
    val naviTrip = NaviHelperTripSource(application)
    val simTrip = SimulatedTripSource(appScope)
    val tripRepository = TripRepository(naviTrip, simTrip, appScope)

    // ── 위치 (FusedLocation) ────────────────────────────────────
    val fusedLocation = FusedLocationSource(application)
    val locationRepository = LocationRepository(
        fused = fusedLocation,
        scope = appScope,
        resolverFlow = content.resolverFlow,
        naviAddress = naviTrip.addressHint,
        simPosition = simTrip.position,
        tripKind = tripRepository.activeKind,
    )

    // ── 음성 (TextToSpeech / SpeechToText) ──────────────────────
    val pleosSpeech = PleosSpeechEngine(application, BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET, clock)
    val androidSpeech = AndroidSpeechEngine(application, clock)
    val scriptedSpeech = ScriptedSpeechEngine(clock)
    val speechRouter = SpeechRouter(pleosSpeech, androidSpeech, scriptedSpeech, appScope)
    val audioFocus = AudioFocusHelper(application)

    val coordinator = GameCoordinator(this)

    private var started = false

    fun start() {
        if (started) return
        started = true
        AppLog.i("App", "AppContainer start (${BuildConfig.VERSION_NAME}, debug=${BuildConfig.DEBUG})")
        carRepository.start()
        tripRepository.start()
        speechRouter.initialize()
        locationRepository.start()
        coordinator.initialize()
    }

    fun onPermissionsChanged() {
        aaosCar.onPermissionsChanged()
        locationRepository.start()
        androidSpeech.refreshSttAvailability()
    }

    fun setForeground(foreground: Boolean) {
        aaosCar.setForeground(foreground)
        tripRepository.setForeground(foreground)
        if (foreground) locationRepository.start() else locationRepository.pause()
    }

    /** 액티비티가 실제로 종료(finish)될 때만 SDK 를 해제한다 (Back 은 moveTaskToBack 이라 해당 없음). */
    fun release() {
        if (!started) return
        started = false
        AppLog.i("App", "AppContainer release")
        coordinator.shutdown()
        speechRouter.release()
        locationRepository.release()
        carRepository.stop()
        tripRepository.stop()
    }
}
