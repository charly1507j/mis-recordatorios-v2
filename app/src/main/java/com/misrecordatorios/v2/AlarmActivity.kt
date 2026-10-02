package com.misrecordatorios.v2

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {

    private var reminderId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reminderId = intent.getLongExtra("id", -1L)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.statusBarColor = Color.rgb(30, 27, 75)
        window.navigationBarColor = Color.rgb(30, 27, 75)

        val list = ReminderStore.load(this)
        val reminder = list.firstOrNull { it.id == reminderId }

        if (reminder == null) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 40, 30, 40)
            setBackgroundColor(Color.rgb(79, 70, 229))
        }

        val icon = TextView(this).apply {
            text = "⏰"
            textSize = 64f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val label = TextView(this).apply {
            text = "RECORDATORIO"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }

        val title = TextView(this).apply {
            text = reminder.title
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(10, 25, 10, 15)
        }

        val notes = TextView(this).apply {
            text = reminder.notes
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(10, 0, 10, 25)
        }

        val button = TextView(this).apply {
            text = "✓  YA LO HICE"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(79, 70, 229))
            setTypeface(null, Typeface.BOLD)
            setPadding(30, 22, 30, 22)
            setBackgroundColor(Color.WHITE)

            setOnClickListener {
                stopAlarm()
            }
        }

        root.addView(icon)
        root.addView(label)
        root.addView(title)

        if (reminder.notes.isNotBlank()) {
            root.addView(notes)
        }

        root.addView(button)

        setContentView(root)
    }

    private fun stopAlarm() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(reminderId.toInt())

        val list = ReminderStore.load(this)
        val reminder = list.firstOrNull { it.id == reminderId }

        if (reminder != null && reminder.repeat == "none") {
            reminder.enabled = false
            ReminderStore.save(this, list)
        }

        finish()
    }

    override fun onBackPressed() {
        stopAlarm()
    }
}
