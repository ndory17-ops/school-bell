
package com.schoolbell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(
            "school_bell", Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean("enabled", false)) return

        val serviceIntent = Intent(
            context, BellService::class.java
        ).apply {
            action = "START"
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // قد تمنع بعض إصدارات أندرويد التشغيل التلقائي
        }
    }
}
