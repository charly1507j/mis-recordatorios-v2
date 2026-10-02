package com.misrecordatorios.v2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra("id", -1L)

        if (id < 0) return

        val list = ReminderStore.load(context)

        val reminder =
            list.firstOrNull { it.id == id }
                ?: return

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        /*
         * ==========================================
         * TONO DE LLAMADA DEL TELÉFONO
         * ==========================================
         *
         * En lugar del sonido multimedia, utilizamos
         * el tono de llamada configurado en el teléfono.
         */

        val ringtoneUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_RINGTONE
            )

        /*
         * Canal independiente para las alarmas.
         *
         * El ID diferente permite que Android no reutilice
         * la configuración antigua del canal "reminder_alarm".
         */

        val channelId = "reminder_call_alarm"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            /*
             * Si el canal ya existe, Android conserva su
             * configuración. Por eso solo lo creamos si
             * todavía no existe.
             */

            if (
                notificationManager.getNotificationChannel(
                    channelId
                ) == null
            ) {

                val audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()

                val channel =
                    NotificationChannel(
                        channelId,
                        "Llamadas de recordatorios",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {

                        description =
                            "Sonido de los recordatorios"

                        setSound(
                            ringtoneUri,
                            audioAttributes
                        )

                        enableVibration(true)

                        vibrationPattern =
                            longArrayOf(
                                0,
                                700,
                                300,
                                700,
                                300,
                                1000
                            )

                        lockscreenVisibility =
                            Notification.VISIBILITY_PUBLIC
                    }

                notificationManager.createNotificationChannel(
                    channel
                )
            }
        }

        /*
         * ==========================================
         * ACTIVIDAD DE ALARMA A PANTALLA COMPLETA
         * ==========================================
         */

        val alarmIntent =
            Intent(
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

        /*
         * ==========================================
         * BOTÓN / ACCESO A LA APP
         * ==========================================
         */

        val mainIntent =
            Intent(
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

        /*
         * ==========================================
         * NOTIFICACIÓN
         * ==========================================
         */

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification.Builder(
                    context,
                    channelId
                )

            } else {

                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

        builder
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle(
                "⏰ ${reminder.title}"
            )
            .setContentText(
                if (reminder.notes.isBlank()) {
                    "Es hora de tu recordatorio"
                } else {
                    reminder.notes
                }
            )
            .setCategory(
                Notification.CATEGORY_ALARM
            )
            .setPriority(
                Notification.PRIORITY_MAX
            )
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(
                mainPendingIntent
            )
            .setVisibility(
                Notification.VISIBILITY_PUBLIC
            )

        /*
         * ==========================================
         * PANTALLA COMPLETA
         * ==========================================
         */

        if (Build.VERSION.SDK_INT < 34) {

            builder.setFullScreenIntent(
                alarmPendingIntent,
                true
            )

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

        /*
         * Mostrar la alarma.
         */

        notificationManager.notify(
            id.toInt(),
            builder.build()
        )

        /*
         * ==========================================
         * PROGRAMAR SIGUIENTE REPETICIÓN
         * ==========================================
         */

        if (reminder.repeat != "none") {

            reminder.timeMillis =
                AlarmScheduler.nextTrigger(
                    reminder.copy(
                        timeMillis =
                            reminder.timeMillis
                    )
                )

            ReminderStore.save(
                context,
                list
            )

            AlarmScheduler.schedule(
                context,
                reminder
            )

        } else {

            /*
             * Recordatorio de una sola vez.
             */

            reminder.enabled = false

            ReminderStore.save(
                context,
                list
            )
        }
    }
}
