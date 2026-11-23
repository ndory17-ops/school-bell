
package com.schoolbell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BellService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var running = false
    private var lastRing = ""

    private val bellTimes = setOf(
        "08:00", "08:40",
        "08:45", "09:25",
        "09:30", "10:10",
        "10:15", "10:55",
        "11:00", "11:40",
        "11:45", "12:25"
    )

    private val checker = object : Runnable {
        override fun run() {
            if (!running) return

            val now = Calendar.getInstance()
            val day = now.get(Calendar.DAY_OF_WEEK)
            val time = SimpleDateFormat(
                "HH:mm", Locale.US
            ).format(Date())

            val key = "$day-$time"

            if (day in Calendar.SUNDAY..Calendar.THURSDAY &&
                time in bellTimes && key != lastRing
            ) {
                lastRing = key
                playBell()
            }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == "STOP") {
            stopBellService()
            return START_NOT_STICKY
        }

        startForeground(1, makeNotification())

        if (!running) {
            running = true

            val power = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SchoolBell::WakeLock"
            )
            wakeLock?.acquire()

            handler.post(checker)
        }

        return START_STICKY
    }

    private fun playBell() {
        try {
            player?.release()

            val saved = getSharedPreferences(
                "school_bell", MODE_PRIVATE
            ).getString("sound_uri", null)

            player = if (saved != null) {
                MediaPlayer.create(this, Uri.parse(saved))
            } else {
                val uri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_ALARM
                )
                MediaPlayer.create(this, uri)
            }

            player?.setOnCompletionListener {
                it.release()
                if (player === it) player = null
            }

            player?.start()
        } catch (_: Exception) {
            player?.release()
            player = null
        }
    }

    private fun stopBellService() {
        running = false
        handler.removeCallbacks(checker)

        player?.release()
        player = null

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "school_bell_channel",
                "جرس المدرسة",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun makeNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "school_bell_channel")
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("جرس المدرسة")
            .setContentText("جدول الجرس يعمل")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(checker)
        player?.release()
        player = null

        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
