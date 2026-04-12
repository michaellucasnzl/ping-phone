package com.michaellucas.pingphone.viewmodel

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaellucas.pingphone.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val triggerCode: StateFlow<String> = repository.triggerCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_TRIGGER_CODE)

    val volumePercent: StateFlow<Int> = repository.volumePercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_VOLUME_PERCENT)

    val alarmSoundUri: StateFlow<String> = repository.alarmSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _permissionsGranted = MutableStateFlow(PermissionState())
    val permissionsGranted: StateFlow<PermissionState> = _permissionsGranted.asStateFlow()

    fun saveTriggerCode(code: String) = viewModelScope.launch { repository.saveTriggerCode(code) }
    fun saveVolumePercent(percent: Int) = viewModelScope.launch { repository.saveVolumePercent(percent) }
    fun saveAlarmSoundUri(uri: String) = viewModelScope.launch { repository.saveAlarmSoundUri(uri) }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val nm = context.getSystemService(NotificationManager::class.java)

        val hasSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasDnd = nm.isNotificationPolicyAccessGranted

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _permissionsGranted.value = PermissionState(
            sms = hasSms,
            dnd = hasDnd,
            notifications = hasNotifications
        )
    }

    data class PermissionState(
        val sms: Boolean = false,
        val dnd: Boolean = false,
        val notifications: Boolean = true
    ) {
        val allGranted: Boolean get() = sms && dnd && notifications
    }
}
