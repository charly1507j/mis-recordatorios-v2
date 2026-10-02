package com.misrecordatorios.v2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderStore {
    private const val PREFS = "reminders"
    private const val KEY = "items"
    fun load(ctx: Context): MutableList<Reminder> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val a = JSONArray(raw); val out = mutableListOf<Reminder>()
        for (i in 0 until a.length()) { val o=a.getJSONObject(i); out += Reminder(o.getLong("id"),o.getString("title"),o.optString("notes"),o.getLong("time"),o.optString("repeat","none"),o.optBoolean("enabled",true)) }
        return out
    }
    fun save(ctx: Context, list: List<Reminder>) {
        val a=JSONArray(); list.forEach { r -> a.put(JSONObject().apply { put("id",r.id); put("title",r.title); put("notes",r.notes); put("time",r.timeMillis); put("repeat",r.repeat); put("enabled",r.enabled) }) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply()
    }
}
