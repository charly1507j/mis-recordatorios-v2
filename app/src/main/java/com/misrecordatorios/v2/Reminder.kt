package com.misrecordatorios.v2

data class Reminder(val id: Long, var title: String, var notes: String, var timeMillis: Long, var repeat: String, var enabled: Boolean = true)
