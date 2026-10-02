package com.misrecordatorios.v2

import android.content.*
class BootReceiver : BroadcastReceiver(){ override fun onReceive(context: Context,intent:Intent){ ReminderStore.load(context).filter{it.enabled}.forEach{ AlarmScheduler.schedule(context,it) } } }
