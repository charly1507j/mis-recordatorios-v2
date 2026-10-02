package com.misrecordatorios.v2

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra("id", -1L)
        if (id < 0) return

        val list = ReminderStore.load(context)
        val reminder = list.firstOrNull { it.id == id } ?: return

        val notificationManager =
            context.getSystemService(NotificationManager::class.java)

        val channelId = "reminder_alarm"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val sound =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val channel = NotificationChannel(
                channelId,
                "Alarmas de recordatorios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description = "Alarmas de Mis Recordatorios"

                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )

                enableVibration(true)
                vibrationPattern =
                    longArrayOf(0, 500, 300, 500, 300, 800)
            }

            notificationManager.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(
            context,
            AlarmActivity::class.java
        ).apply {
            putExtra("id", id)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val alarmPendingIntent =
            PendingIntent.getActivity(
                context,
                id.toInt() + 200000,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
            )

        val mainIntent = Intent(
            context,
            MainActivity::class.java
        )

        val mainPendingIntent =
            PendingIntent.getActivity(
                context,
                id.toInt() + 100000,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ ${reminder.title}")
                .setContentText(
                    if (reminder.notes.isBlank())
                        "Es hora de tu recordatorio"
                    else
                        reminder.notes
                )
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent)
                .setVibrate(
                    longArrayOf(0, 500, 300, 500, 300, 800)
                )

        if (Build.VERSION.SDK_INT < 34) {
            builder.setFullScreenIntent(alarmPendingIntent, true)
        } else {
            val canUseFullScreen =
                notificationManager.canUseFullScreenIntent()

            if (canUseFullScreen) {
                builder.setFullScreenIntent(
                    alarmPendingIntent,
                    true
                )
            }
        }

        notificationManager.notify(id.toInt(), builder.build())

        if (reminder.repeat != "none") {

            reminder.timeMillis =
                AlarmScheduler.nextTrigger(
                    reminder.copy(
                        timeMillis = reminder.timeMillis
                    )
                )

            ReminderStore.save(context, list)
            AlarmScheduler.schedule(context, reminder)

        } else {

            reminder.enabled = false
            ReminderStore.save(context, list)
        }
    }
}
