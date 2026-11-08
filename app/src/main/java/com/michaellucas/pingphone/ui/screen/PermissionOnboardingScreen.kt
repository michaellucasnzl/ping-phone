package com.michaellucas.pingphone.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michaellucas.pingphone.R
import com.michaellucas.pingphone.viewmodel.SettingsViewModel

@Composable
fun PermissionOnboardingScreen(
    permissions: SettingsViewModel.PermissionState,
    onPermissionsUpdated: () -> Unit
) {
    val context = LocalContext.current

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onPermissionsUpdated() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onPermissionsUpdated() }

    val showNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val showBattery = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val showFullScreen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !permissions.fullScreenIntent

    // Sequential step numbers with no gaps when conditional steps are hidden.
    val smsStep = 1
    val dndStep = 2
    val notificationsStep = 3
    val batteryStep = if (showNotifications) 4 else 3
    val fullScreenStep = batteryStep + 1

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Step 1: SMS
            PermissionStep(
                stepNumber = smsStep,
                title = stringResource(R.string.permission_sms_title),
                description = stringResource(R.string.permission_sms_desc),
                granted = permissions.sms,
                onGrant = {
                    smsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
                }
            )

            // Step 2: DND access (requires special settings intent — no runtime dialog)
            PermissionStep(
                stepNumber = dndStep,
                title = stringResource(R.string.permission_dnd_title),
                description = stringResource(R.string.permission_dnd_desc),
                granted = permissions.dnd,
                onGrant = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            )

            // Step 3: Notifications (Android 13+ only)
            if (showNotifications) {
                PermissionStep(
                    stepNumber = notificationsStep,
                    title = stringResource(R.string.permission_notifications_title),
                    description = stringResource(R.string.permission_notifications_desc),
                    granted = permissions.notifications,
                    onGrant = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            // Step 4: Battery optimization exemption (Android 12+ only)
            if (showBattery) {
                PermissionStep(
                    stepNumber = batteryStep,
                    title = stringResource(R.string.permission_battery_title),
                    description = stringResource(R.string.permission_battery_desc),
                    granted = permissions.batteryOptimization,
                    onGrant = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (e: ActivityNotFoundException) {
                            // No system handler for the request; nothing else we can do.
                        }
                    }
                )
            }

            // Step 5: Full-screen intent access (Android 14+, only when not granted)
            if (showFullScreen) {
                PermissionStep(
                    stepNumber = fullScreenStep,
                    title = stringResource(R.string.permission_fullscreen_title),
                    description = stringResource(R.string.permission_fullscreen_desc),
                    granted = permissions.fullScreenIntent,
                    onGrant = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (e: ActivityNotFoundException) {
                            // No system handler for the setting; nothing else we can do.
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (permissions.allGranted) {
                Button(
                    onClick = onPermissionsUpdated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.all_set))
                }
            }
        }
    }
}

@Composable
private fun PermissionStep(
    stepNumber: Int,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (granted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (granted) "✓" else stepNumber.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!granted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onGrant) {
                        Text(stringResource(R.string.grant))
                    }
                }
            }
        }
    }
}
