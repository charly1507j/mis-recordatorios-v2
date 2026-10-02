package com.misrecordatorios.v2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object AlarmScheduler {
    fun exactAllowed(ctx: Context): Boolean = Build.VERSION.SDK_INT < 31 || (ctx.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)
    fun requestExactAccess(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${ctx.packageName}")))
    }
    fun schedule(ctx: Context, r: Reminder) {
        if (!r.enabled) return
        val am=ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi=pending(ctx,r.id)
        val trigger=nextTrigger(r)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) return
        val info=AlarmManager.AlarmClockInfo(trigger, pi)
        am.setAlarmClock(info, pi)
    }
    fun cancel(ctx: Context, id: Long) { ctx.getSystemService(AlarmManager::class.java)?.cancel(pending(ctx,id)) }
    private fun pending(ctx: Context,id:Long): PendingIntent = PendingIntent.getBroadcast(ctx,id.toInt(),Intent(ctx,AlarmReceiver::class.java).putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun nextTrigger(r: Reminder): Long {
        val now=System.currentTimeMillis(); var t=r.timeMillis
        if (t>now) return t
        val cal=java.util.Calendar.getInstance().apply{timeInMillis=t}
        val n=java.util.Calendar.getInstance()
        when(r.repeat){
            "daily" -> while(t<=now){cal.add(java.util.Calendar.DAY_OF_YEAR,1);t=cal.timeInMillis}
            "weekly" -> while(t<=now){cal.add(java.util.Calendar.WEEK_OF_YEAR,1);t=cal.timeInMillis}
            "monthly" -> while(t<=now){cal.add(java.util.Calendar.MONTH,1);t=cal.timeInMillis}
            else -> return t
        }
        return t
    }
}
