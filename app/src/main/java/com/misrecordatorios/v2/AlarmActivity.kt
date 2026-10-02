package com.misrecordatorios.v2

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {

    private var reminderId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reminderId = intent.getLongExtra("id", -1L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

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

        root.addView(icon)
        root.addView(label)
        root.addView(title)

        if (reminder.notes.isNotBlank()) {
            val notes = TextView(this).apply {
                text = reminder.notes
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(10, 0, 10, 25)
            }

            root.addView(notes)
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
                showConfirmation()
            }
        }

        root.addView(button)

        setContentView(root)
    }

    private fun showConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Estás seguro que ya lo hiciste?")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("SÍ, YA LO HICE") { _, _ ->
                stopAlarm()
            }
            .setCancelable(false)
            .show()
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showConfirmation()
    }
}
