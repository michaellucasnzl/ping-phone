package com.michaellucas.pingphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.michaellucas.pingphone.service.MonitoringNotificationHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            MonitoringNotificationHelper.showMonitoringNotification(context)
        }
    }
}
