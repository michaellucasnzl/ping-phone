package com.michaellucas.pingphone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pingphone_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val TRIGGER_CODE = stringPreferencesKey("trigger_code")
        val VOLUME_PERCENT = intPreferencesKey("volume_percent")
        val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
    }

    val triggerCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRIGGER_CODE] ?: DEFAULT_TRIGGER_CODE
    }

    val volumePercent: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.VOLUME_PERCENT] ?: DEFAULT_VOLUME_PERCENT
    }

    val alarmSoundUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALARM_SOUND_URI] ?: ""
    }

    suspend fun saveTriggerCode(code: String) {
        if (code.isBlank() || code.length > 50) return
        context.dataStore.edit { prefs -> prefs[Keys.TRIGGER_CODE] = code }
    }

    suspend fun saveVolumePercent(percent: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.VOLUME_PERCENT] = percent }
    }

    suspend fun saveAlarmSoundUri(uri: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ALARM_SOUND_URI] = uri }
    }

    companion object {
        const val DEFAULT_TRIGGER_CODE = "777"
        const val DEFAULT_VOLUME_PERCENT = 70
    }
}
