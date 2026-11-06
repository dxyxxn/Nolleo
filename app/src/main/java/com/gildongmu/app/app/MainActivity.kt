package com.gildongmu.app.app

import android.Manifest
import android.content.pm.PackageManager
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
import com.gildongmu.app.core.car.AaosCarStateSource
import com.gildongmu.app.core.location.FusedLocationSource
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.ui.GildongmuRoot
import com.gildongmu.app.ui.MainViewModel
import com.gildongmu.app.ui.theme.GildongmuTheme
import kotlinx.coroutines.launch

/** 단일 액티비티. 화면은 Compose, 게임 세션은 앱 범위(AppContainer.coordinator)에 있다. */
class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as GildongmuApp).container
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
            GildongmuTheme {
                GildongmuRoot(container, viewModel)
            }
        }
    }

    private fun requestRuntimePermissions() {
        // dangerous: android.car.permission.CAR_SPEED, pleos.car.permission.FUSED_LOCATION
        val needed = listOf(AaosCarStateSource.SPEED_PERMISSION, FusedLocationSource.PERMISSION).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    override fun onStart() {
        super.onStart()
        container.setForeground(true)
        container.coordinator.onForeground()
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
