
package com.schoolbell

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var clock: TextView
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val clockTask = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat(
                "hh:mm:ss a", Locale.getDefault()
            ).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("school_bell", MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFFF3F5F8.toInt())
        }

        val title = TextView(this).apply {
            text = "🔔 جرس المدرسة"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF183153.toInt())
        }

        clock = TextView(this).apply {
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
            setPadding(0, 35, 0, 25)
        }

        status = TextView(this).apply {
            text = "الجرس متوقف"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFF555555.toInt())
        }

        val start = Button(this).apply {
            text = "▶ تشغيل جدول الجرس"
            textSize = 18f
            setOnClickListener {
                val intent = Intent(this@MainActivity, BellService::class.java)
                intent.action = "START"
                startForegroundService(intent)
                status.text = "الجرس يعمل"
            }
        }

        val stop = Button(this).apply {
            text = "■ إيقاف الجرس"
            textSize = 18f
            setOnClickListener {
                val intent = Intent(this@MainActivity, BellService::class.java)
                intent.action = "STOP"
                startService(intent)
                status.text = "الجرس متوقف"
            }
        }

        val choose = Button(this).apply {
            text = "🎵 اختيار نغمة من الهاتف"
            textSize = 17f
            setOnClickListener {
                val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(picker, 100)
            }
        }

        val selected = TextView(this).apply {
            text = if (prefs.contains("sound_uri"))
                "تم حفظ نغمة مخصصة"
            else
                "النغمة الافتراضية"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF555555.toInt())
        }

        layout.addView(title)
        layout.addView(clock)
        layout.addView(status)
        layout.addView(start)
        layout.addView(stop)
        layout.addView(choose)
        layout.addView(selected)

        setContentView(layout)
        handler.post(clockTask)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            getSharedPreferences("school_bell", MODE_PRIVATE)
                .edit()
                .putString("sound_uri", uri.toString())
                .apply()

            Toast.makeText(
                this, "تم حفظ النغمة", Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTask)
        super.onDestroy()
    }
}
