package com.nolleo.app.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nolleo.app.core.car.AaosCarStateSource
import com.nolleo.app.core.location.FusedLocationSource
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.ui.NolleoRoot
import com.nolleo.app.ui.MainViewModel
import com.nolleo.app.ui.theme.NolleoTheme
import kotlinx.coroutines.launch

/** 단일 액티비티. 화면은 Compose, 게임 세션은 앱 범위(AppContainer.coordinator)에 있다. */
class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as NolleoApp).container
    private val viewModel: MainViewModel by viewModels()
    private var micPermissionAsked = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            AppLog.i("Permission", result.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" })
            container.onPermissionsChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.start()

        // CCP Back (DEV_GUIDE §7.4): 최상단에서는 종료하지 않고 숨긴다. 하위 화면·패널은 Compose BackHandler 가 먼저 받는다.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        requestRuntimePermissions()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.coordinator.needsMicPermission.collect { needed ->
                    // RECORD_AUDIO 는 Android 폴백 인식기가 선택될 때만 요청한다
                    if (needed && !micPermissionAsked) {
                        micPermissionAsked = true
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                }
            }
        }

        setContent {
            NolleoTheme {
                NolleoRoot(container, viewModel)
            }
        }
    }

    private fun requestRuntimePermissions() {
        // dangerous: android.car.permission.CAR_SPEED, pleos.car.permission.FUSED_LOCATION
        // FUSED_LOCATION 은 위치 서비스가 있을 때만 요청한다 (샘플 순서: isSupported → 퍼미션 → initialize)
        val candidates = buildList {
            add(AaosCarStateSource.SPEED_PERMISSION)
            if (container.fusedLocation.isSupported()) add(FusedLocationSource.PERMISSION)
        }
        val needed = candidates.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    override fun onStart() {
        super.onStart()
        container.setForeground(true)
        container.coordinator.onForeground()
        // 창이 자리를 잡은 뒤에 판단한다
        window.decorView.post { checkWindowPlacement(cameToFront = true) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkWindowPlacement(cameToFront = false)
    }

    private var lastWindow: WindowPlacement.Box? = null

    /** 3분할 화면에서 놀레오가 운전석 쪽 창에 있으면 맨 오른쪽(조수석 쪽)으로 옮긴다 — [WindowPlacement] */
    private fun checkWindowPlacement(cameToFront: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val bounds = windowManager.currentWindowMetrics.bounds
        val now = WindowPlacement.Box(bounds.left, bounds.right)
        val screenRight = windowManager.maximumWindowMetrics.bounds.right
        val previous = lastWindow
        lastWindow = now
        if (WindowPlacement.shouldMoveToPassengerSide(previous, now, screenRight, cameToFront)) {
            moveGameToPassengerSide("${now.left}..${now.right}/$screenRight, ${if (cameToFront) "앞으로 나옴" else "창 크기 변경"}")
        }
    }

    /**
     * 새로 실행된 앱은 primary(운전석 쪽) 창으로 간다(DEV_GUIDE §6.3) → 기본 지도(내비) 앱을 실행하면
     * 놀레오는 조수석 쪽 창으로 밀려나 Driving view | 내비 | 놀레오 가 된다. 앱 이름을 하드코딩하지 않고 표준 카테고리로 찾는다.
     * (NaviHelper requestNavigationScreen(Foreground) 는 내비가 이미 보이면 다시 실행하지 않아 자리가 바뀌지 않음 — 에뮬레이터 확인)
     */
    private fun moveGameToPassengerSide(reason: String) {
        val maps = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = try {
            startActivity(maps)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
        AppLog.i("Window", "3분할: 놀레오가 운전석 쪽 창($reason) → 기본 지도 앱을 앞으로, 게임은 맨 오른쪽 (launched=$launched)")
    }

    override fun onStop() {
        // 백그라운드 전환 = 화면 갱신·STT 중단, 포커스 반납, 상태 저장 (Suspend 진입 포함)
        container.coordinator.onBackground()
        container.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) container.release()
        super.onDestroy()
    }
}
