package com.michaellucas.pingphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.michaellucas.pingphone.data.SettingsRepository
import com.michaellucas.pingphone.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val triggerCode = repository.triggerCode.first()
                // Use word-boundary regex so "777" won't match inside "7779" or "17777"
                val matched = triggerCode.isNotBlank() &&
                    Regex("\\b${Regex.escape(triggerCode)}\\b").containsMatchIn(fullBody)
                if (matched) {
                    val serviceIntent = Intent(context, AlarmService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
