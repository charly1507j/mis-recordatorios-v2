package com.misrecordatorios.v2

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DateFormat
import java.util.*

class MainActivity : Activity() {
    private val list = mutableListOf<Reminder>()
    private lateinit var container: LinearLayout
    private lateinit var countText: TextView
    private val primary = Color.rgb(79, 70, 229)
    private val primaryDark = Color.rgb(55, 48, 163)
    private val text = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(107, 114, 128)
    private val surface = Color.WHITE
    private val background = Color.rgb(246, 247, 251)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = primaryDark
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        title = "Mis Recordatorios"
        list.addAll(ReminderStore.load(this))
        build()
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            strokeColor?.let { setStroke(dp(1), it) }
        }

    private fun tv(value: String, size: Float, color: Int = text, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
            background = rounded(primary, 26)
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply {
            text = "✓"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = rounded(Color.argb(45, 255, 255, 255), 18)
        }
        top.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleBox.addView(tv("Mis Recordatorios", 24f, Color.WHITE, true))
        titleBox.addView(tv("Tus alarmas, siempre a tiempo", 13f, Color.argb(225, 255, 255, 255)))
        top.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(top)

        val statRow = LinearLayout(this).apply {
            setPadding(0, dp(18), 0, 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        countText = tv("", 13f, Color.WHITE, true)
        statRow.addView(countText, LinearLayout.LayoutParams(0, -2, 1f))
        val exact = TextView(this).apply {
            text = "⚙  Alarmas exactas"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(Color.argb(45, 255, 255, 255), 14)
            setOnClickListener { AlarmScheduler.requestExactAccess(this@MainActivity) }
        }
        statRow.addView(exact)
        header.addView(statRow)

        val headerLp = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(12), dp(12), dp(12), dp(8))
        }
        root.addView(header, headerLp)

        val add = TextView(this).apply {
            text = "＋   Nuevo recordatorio"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(primary, 17)
            elevation = dp(3).toFloat()
            setOnClickListener { editor(null) }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(22), dp(8), dp(22), dp(8))
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(4), dp(22), dp(24))
        }
        scroll.addView(container)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun render() {
        container.removeAllViews()
        val active = list.count { it.enabled }
        countText.text = if (active == 1) "1 recordatorio activo" else "$active recordatorios activos"

        if (list.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(42), dp(24), dp(42))
                background = rounded(surface, 22, Color.rgb(229, 231, 235))
            }
            val icon = tv("⏰", 42f, primary, false).apply { gravity = Gravity.CENTER }
            empty.addView(icon)
            empty.addView(tv("Todo en orden", 20f, text, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            empty.addView(tv("Crea tu primer recordatorio y recibirás una alarma a la hora indicada.", 14f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), 0)
            })
            container.addView(empty)
            return
        }

        val sorted = list.sortedBy { it.timeMillis }
        val today = Calendar.getInstance()
        val todayCount = sorted.count { sameDay(it.timeMillis, today.timeInMillis) }
        val section = tv(if (todayCount > 0) "Próximos  ·  $todayCount hoy" else "Próximos recordatorios", 15f, muted, true)
        section.setPadding(dp(2), dp(4), 0, dp(10))
        container.addView(section)

        sorted.forEach { r ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(17), dp(15), dp(17), dp(15))
                background = rounded(surface, 20, Color.rgb(229, 231, 235))
                elevation = dp(1).toFloat()
                setOnClickListener { editor(r) }
            }

            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val alarmIcon = TextView(this).apply {
                text = "⏰"
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(primary)
                background = rounded(Color.rgb(238, 242, 255), 14)
            }
            row.addView(alarmIcon, LinearLayout.LayoutParams(dp(44), dp(44)))

            val titleBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
            }
            titleBox.addView(tv(r.title, 17f, text, true))
            titleBox.addView(tv(formatDate(r.timeMillis), 13f, muted).apply { setPadding(0, dp(4), 0, 0) })
            row.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))

            val badge = TextView(this).apply {
                text = repeatShort(r.repeat)
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(primaryDark)
                setPadding(dp(9), dp(6), dp(9), dp(6))
                background = rounded(Color.rgb(238, 242, 255), 12)
            }
            row.addView(badge)
            card.addView(row)

            if (r.notes.isNotBlank()) {
                card.addView(tv(r.notes, 13f, muted).apply {
                    setPadding(dp(56), dp(8), dp(4), 0)
                })
            }

            val actionRow = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(56), dp(10), 0, 0)
            }
            val edit = TextView(this).apply {
                text = "Editar"
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(primaryDark)
                setPadding(dp(13), dp(8), dp(13), dp(8))
                background = rounded(Color.rgb(238, 242, 255), 12)
                setOnClickListener { editor(r) }
            }
            val del = TextView(this).apply {
                text = "Eliminar"
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(185, 28, 28))
                setPadding(dp(13), dp(8), dp(13), dp(8))
                background = rounded(Color.rgb(254, 242, 242), 12)
                setOnClickListener { confirmDelete(r) }
            }
            actionRow.addView(edit)
            actionRow.addView(del, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
            card.addView(actionRow)

            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            container.addView(card, lp)
        }
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a }
        val y = Calendar.getInstance().apply { timeInMillis = b }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDate(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))

    private fun repeatShort(s: String) = when (s) {
        "daily" -> "Diario"
        "weekly" -> "Semanal"
        "monthly" -> "Mensual"
        else -> "Una vez"
    }

    private fun editor(existing: Reminder?) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(6), dp(24), 0)
        }
        val title = EditText(this).apply {
            hint = "Título del recordatorio"
            setSingleLine()
            setText(existing?.title ?: "")
        }
        box.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        val notes = EditText(this).apply {
            hint = "Nota opcional"
            setMinLines(2)
            setText(existing?.notes ?: "")
            gravity = Gravity.TOP
        }
        box.addView(notes, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        var cal = Calendar.getInstance()
        if (existing != null) cal.timeInMillis = existing.timeMillis
        val dateBtn = Button(this).apply { text = "Fecha: ${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}" }
        val timeBtn = Button(this).apply { text = String.format("Hora: %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) }
        box.addView(dateBtn)
        box.addView(timeBtn)

        val reps = arrayOf("Una vez", "Cada día", "Cada semana", "Cada mes")
        val repVals = arrayOf("none", "daily", "weekly", "monthly")
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reps)
        if (existing != null) sp.setSelection(repVals.indexOf(existing.repeat).coerceAtLeast(0))
        box.addView(sp)

        dateBtn.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                dateBtn.text = "Fecha: $d/${m + 1}/$y"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        timeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                timeBtn.text = String.format("Hora: %02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nuevo recordatorio" else "Editar recordatorio")
            .setView(box)
            .setPositiveButton("Guardar") { _, _ ->
                if (title.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                var t = cal.timeInMillis
                if (sp.selectedItemPosition > 0 && t <= System.currentTimeMillis()) {
                    val tmp = Reminder(existing?.id ?: System.currentTimeMillis(), title.text.toString(), notes.text.toString(), t, repVals[sp.selectedItemPosition])
                    t = AlarmScheduler.nextTrigger(tmp)
                }
                val r = Reminder(existing?.id ?: System.currentTimeMillis(), title.text.toString().trim(), notes.text.toString().trim(), t, repVals[sp.selectedItemPosition], true)
                existing?.let { AlarmScheduler.cancel(this, it.id); list.removeAll { item -> item.id == r.id } }
                list.add(r)
                ReminderStore.save(this, list)
                if (!AlarmScheduler.exactAllowed(this)) AlarmScheduler.requestExactAccess(this) else AlarmScheduler.schedule(this, r)
                render()
                Toast.makeText(this, "Recordatorio guardado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(r: Reminder) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar recordatorio")
            .setMessage("¿Eliminar “${r.title}”? ")
            .setPositiveButton("Eliminar") { _, _ ->
                AlarmScheduler.cancel(this, r.id)
                list.removeAll { it.id == r.id }
                ReminderStore.save(this, list)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
