package com.michaellucas.pingphone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager

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
            "Monitoring Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows that PingPhone is actively monitoring for the trigger SMS"
        }

        // High-priority channel for the alarm notification
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen alarm notification when the trigger SMS is received"
            setSound(alarmSound, alarmAttributes)
            enableVibration(true)
        }

        nm.createNotificationChannel(monitoringChannel)
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_MONITORING = "monitoring_channel"
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
