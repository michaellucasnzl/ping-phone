package com.michaellucas.pingphone.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.michaellucas.pingphone.PingPhoneApplication
import com.michaellucas.pingphone.R
import com.michaellucas.pingphone.data.SettingsRepository
import com.michaellucas.pingphone.ui.AlarmDismissActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmService : Service() {

    // Coroutine scope tied to the service lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var startupJob: Job? = null
    // Guard against multiple simultaneous alarm starts (e.g. two trigger SMS in quick succession)
    @Volatile private var isAlarming = false

    // State saved before overriding audio so we can restore it on dismiss
    private var savedRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var savedAlarmVolume: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAlarm()
            else -> startAlarm()
        }
        return START_NOT_STICKY
    }

    private fun startAlarm() {
        if (isAlarming) return
        isAlarming = true
        // Must call startForeground() quickly to satisfy Android requirements
        startForeground(NOTIFICATION_ID, buildAlarmNotification())

        val audioManager = getSystemService(AudioManager::class.java)
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Save current audio state for restore on dismiss
        savedRingerMode = audioManager.ringerMode
        savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Override DND if permission was granted
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        // Force ringer to normal so the alarm is audible
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

        val repository = SettingsRepository(this)
        startupJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val volumePercent = repository.volumePercent.first()
                val alarmSoundUriString = repository.alarmSoundUri.first()

                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetVolume = (maxVolume * volumePercent / 100.0).toInt().coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

                val alarmUri: Uri = when {
                    alarmSoundUriString.isNotEmpty() -> Uri.parse(alarmSoundUriString)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: run { stopSelf(); return@launch }
                }

                // Acquire partial wake lock so alarm plays with screen off (max 10 minutes)
                val powerManager = getSystemService(PowerManager::class.java)
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PingPhone:AlarmWakeLock"
                ).also { it.acquire(10 * 60 * 1000L) }

                withContext(Dispatchers.Main) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(this@AlarmService, alarmUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                stopSelf()
            }
        }
    }

    private fun stopAlarm() {
        isAlarming = false
        startupJob?.cancel()

        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        // Restore previous audio state
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
        audioManager.ringerMode = savedRingerMode

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildAlarmNotification() = run {
        val dismissActivityIntent = Intent(this, AlarmDismissActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, dismissActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopServicePendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        NotificationCompat.Builder(this, PingPhoneApplication.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, getString(R.string.dismiss), stopServicePendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancels serviceScope and all child jobs (including startupJob)
        serviceScope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    companion object {
        const val ACTION_STOP = "com.michaellucas.pingphone.ACTION_STOP_ALARM"
        const val NOTIFICATION_ID = 1002
    }
}
