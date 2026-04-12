package com.michaellucas.pingphone.ui.screen

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michaellucas.pingphone.R
import com.michaellucas.pingphone.service.AlarmService
import com.michaellucas.pingphone.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val triggerCode by viewModel.triggerCode.collectAsStateWithLifecycle()
    val volumePercent by viewModel.volumePercent.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsGranted.collectAsStateWithLifecycle()

    // Local copy so the text field updates on each keystroke but only saves on explicit tap
    var triggerCodeInput by remember(triggerCode) { mutableStateOf(triggerCode) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        @Suppress("DEPRECATION")
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        } else {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        viewModel.saveAlarmSoundUri(uri?.toString() ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status card
            StatusCard(allGranted = permissions.allGranted)

            // Trigger code
            OutlinedTextField(
                value = triggerCodeInput,
                onValueChange = { triggerCodeInput = it },
                label = { Text(stringResource(R.string.trigger_code_label)) },
                supportingText = { Text(stringResource(R.string.trigger_code_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (triggerCodeInput != triggerCode) {
                        TextButton(onClick = { viewModel.saveTriggerCode(triggerCodeInput) }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            )

            // Volume slider
            Column {
                Text(
                    text = stringResource(R.string.volume_label, volumePercent),
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { viewModel.saveVolumePercent(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 17,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Alarm sound picker
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.alarm_sound_label)) },
                    supportingContent = {
                        val displayName = if (alarmSoundUri.isNotEmpty()) {
                            RingtoneManager.getRingtone(context, Uri.parse(alarmSoundUri))
                                ?.getTitle(context) ?: alarmSoundUri
                        } else {
                            stringResource(R.string.default_alarm_sound)
                        }
                        Text(displayName)
                    },
                    trailingContent = {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.pick_alarm_sound))
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    if (alarmSoundUri.isNotEmpty()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmSoundUri))
                                    }
                                }
                                ringtoneLauncher.launch(intent)
                            }
                        ) { Text(stringResource(R.string.change)) }
                    }
                )
            }

            // Test button
            Button(
                onClick = {
                    context.startForegroundService(Intent(context, AlarmService::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.test_alarm))
            }

            // Permission warning banners
            if (!permissions.sms) {
                WarningCard(stringResource(R.string.warning_sms))
            }
            if (!permissions.dnd) {
                WarningCard(stringResource(R.string.warning_dnd))
            }
        }
    }
}

@Composable
private fun StatusCard(allGranted: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = if (allGranted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (allGranted)
                    stringResource(R.string.status_monitoring)
                else
                    stringResource(R.string.status_incomplete),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun WarningCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
