package com.michaellucas.pingphone.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michaellucas.pingphone.service.MonitoringNotificationHelper
import com.michaellucas.pingphone.ui.screen.MainScreen
import com.michaellucas.pingphone.ui.screen.PermissionOnboardingScreen
import com.michaellucas.pingphone.ui.theme.PingPhoneTheme
import com.michaellucas.pingphone.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Seed permission state before first composition so there's no blank frame
        viewModel.refreshPermissions()
        MonitoringNotificationHelper.showMonitoringNotification(this)

        setContent {
            PingPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissions by viewModel.permissionsGranted.collectAsStateWithLifecycle()

                    if (!permissions.allGranted) {
                        PermissionOnboardingScreen(
                            permissions = permissions,
                            onPermissionsUpdated = { viewModel.refreshPermissions() }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}
