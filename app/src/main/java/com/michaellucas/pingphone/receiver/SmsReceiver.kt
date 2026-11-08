package com.michaellucas.pingphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.michaellucas.pingphone.data.SettingsRepository
import com.michaellucas.pingphone.data.TriggerMatcher
import com.michaellucas.pingphone.service.AlarmService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Concatenate all parts of a potentially multi-segment SMS before checking
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        val repository = SettingsRepository(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val triggerCode = repository.triggerCode.first()
                val matched = TriggerMatcher.matches(fullBody, triggerCode)
                if (matched) {
                    val serviceIntent = Intent(context, AlarmService::class.java)
                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        // Foreground-service start can be rejected on Android 12+ background-restriction
                        // enforcement. Log without SMS contents; the user's battery-optimization exemption
                        // (requested in onboarding) is the documented workaround.
                        Log.w(TAG, "Failed to start AlarmService: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to process SMS: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
