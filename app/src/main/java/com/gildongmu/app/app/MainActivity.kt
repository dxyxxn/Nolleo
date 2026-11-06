package com.gildongmu.app.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.core.car.AaosCarStateSource
import com.gildongmu.app.ui.theme.GdColors
import com.gildongmu.app.ui.theme.GildongmuTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as GildongmuApp).container

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            container.onPermissionsChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.start()

        // CCP Back (DEV_GUIDE §7.4): 최상단에서는 종료하지 않고 숨긴다. 하위 화면은 Compose BackHandler 가 먼저 받는다.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        permissionLauncher.launch(arrayOf(AaosCarStateSource.SPEED_PERMISSION))

        setContent {
            GildongmuTheme {
                val decision by container.modeController.decision.collectAsStateWithLifecycle()
                val signals by container.carRepository.signals.collectAsStateWithLifecycle()
                val ux by container.carRepository.ux.collectAsStateWithLifecycle()
                val session by container.modeController.sessionActive.collectAsStateWithLifecycle()
                Column(
                    Modifier.fillMaxSize().background(GdColors.Background).padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("길동무 — M1", style = MaterialTheme.typography.headlineLarge, color = GdColors.Text)
                    Text("모드: ${decision.mode.label}", style = MaterialTheme.typography.headlineMedium, color = GdColors.Primary)
                    Text("주행: ${decision.driveState.label} (${decision.reason})", color = GdColors.Text, style = MaterialTheme.typography.titleLarge)
                    Text("신호: $signals", color = GdColors.Muted, style = MaterialTheme.typography.bodyMedium)
                    Text("규제: DO=${ux.requiresDO} bits=${ux.activeRestrictions}", color = GdColors.Muted, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { container.modeController.sessionActive.value = !session },
                        modifier = Modifier.heightIn(min = 76.dp),
                    ) { Text(if (session) "세션 종료" else "세션 시작") }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.setForeground(true)
    }

    override fun onStop() {
        container.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) container.release()
        super.onDestroy()
    }
}
