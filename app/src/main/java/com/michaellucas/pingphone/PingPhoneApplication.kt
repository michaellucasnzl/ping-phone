package com.michaellucas.pingphone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class PingPhoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Low-priority channel for the persistent "monitoring" status notification
        val monitoringChannel = NotificationChannel(
            CHANNEL_MONITORING,
            getString(R.string.monitoring_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitoring_channel_description)
        }

        // High-priority channel for the alarm notification. It is intentionally soundless and
        // vibration-free: the alarm audio is played by AlarmService's MediaPlayer, so a channel
        // sound would layer a second audio source on top of it.
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM_V2,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alarm_channel_description)
        }

        nm.createNotificationChannel(monitoringChannel)
        nm.createNotificationChannel(alarmChannel)

        // Migrate away from the legacy audible channel. Android persists channel sound/vibration
        // settings across app upgrades, so the old channel must be deleted rather than reused.
        // Create-then-delete ensures the channel exists before deletion is attempted.
        try {
            val legacyChannel = NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.alarm_legacy_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(legacyChannel)
            nm.deleteNotificationChannel(CHANNEL_ALARM)
        } catch (e: Exception) {
            // Defensive: channel migration is best-effort and must not crash app startup.
        }
    }

    companion object {
        const val CHANNEL_MONITORING = "monitoring_channel"
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_ALARM_V2 = "alarm_channel_v2"
    }
}
