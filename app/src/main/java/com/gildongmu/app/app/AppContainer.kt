package com.gildongmu.app.app

import android.app.Application
import com.gildongmu.app.core.Clock
import com.gildongmu.app.core.SystemClockSource
import com.gildongmu.app.core.car.AaosCarStateSource
import com.gildongmu.app.core.car.CarStateRepository
import com.gildongmu.app.core.car.SimulatedCarStateSource
import com.gildongmu.app.core.car.VehicleSdkClient
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.engine.ModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 수동 DI 컨테이너 (지시서 §4: Hilt/kapt 미사용). SDK 싱글톤을 소유하고 앱 수명 동안 공유한다.
 */
class AppContainer(val application: Application) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val clock: Clock = SystemClockSource

    // ── 차량 상태 ────────────────────────────────────────────────
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

    private var started = false

    fun start() {
        if (started) return
        started = true
        AppLog.i("App", "AppContainer start")
        carRepository.start()
    }

    fun onPermissionsChanged() {
        aaosCar.onPermissionsChanged()
    }

    fun setForeground(foreground: Boolean) {
        aaosCar.setForeground(foreground)
    }

    /** 액티비티가 실제로 종료(finish)될 때만 SDK 를 해제한다 (Back 은 moveTaskToBack 이라 해당 없음). */
    fun release() {
        if (!started) return
        started = false
        AppLog.i("App", "AppContainer release")
        carRepository.stop()
    }
}
