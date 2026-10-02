package com.misrecordatorios.v2

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id=intent.getLongExtra("id", -1); if(id<0)return
        val list=ReminderStore.load(context); val r=list.firstOrNull{it.id==id} ?: return
        val nm=context.getSystemService(NotificationManager::class.java)
        val channelId="reminder_alarm"
        if(Build.VERSION.SDK_INT>=26){
            val sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ch=NotificationChannel(channelId,"Alarmas de recordatorios",NotificationManager.IMPORTANCE_HIGH).apply{
                description="Alarmas de Mis Recordatorios"; setSound(sound,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()); enableVibration(true); vibrationPattern=longArrayOf(0,500,300,500,300,800)
            }; nm.createNotificationChannel(ch)
        }
        val open=PendingIntent.getActivity(context, id.toInt()+100000, Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(context,channelId).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("⏰ ${r.title}").setContentText(if(r.notes.isBlank())"Es hora de tu recordatorio" else r.notes).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setAutoCancel(true).setContentIntent(open).setVibrate(longArrayOf(0,500,300,500,300,800)).build()
        nm.notify(id.toInt(),n)
        if(r.repeat!="none"){r.timeMillis=AlarmScheduler.nextTrigger(r.copy(timeMillis=r.timeMillis)); ReminderStore.save(context,list); AlarmScheduler.schedule(context,r)} else { r.enabled=false; ReminderStore.save(context,list) }
    }
}
