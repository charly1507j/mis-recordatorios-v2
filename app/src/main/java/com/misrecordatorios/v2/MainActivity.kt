package com.misrecordatorios.v2

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.*
import java.text.DateFormat
import java.util.*

class MainActivity : Activity() {
    private val list=mutableListOf<Reminder>(); lateinit var container: LinearLayout
    private val blue=0xff4f46e5.toInt()
    override fun onCreate(b:Bundle?){super.onCreate(b); title="Mis Recordatorios"; list.addAll(ReminderStore.load(this)); build(); requestPermissionsIfNeeded(); }
    private fun requestPermissionsIfNeeded(){ if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),10) }
    private fun build(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,24)}
        val head=TextView(this).apply{text="Mis Recordatorios";textSize=28f;setTypeface(null,Typeface.BOLD);setTextColor(0xff111827.toInt())}
        root.addView(head, LinearLayout.LayoutParams(-1,-2));
        val sub=TextView(this).apply{text="Alarmas que suenan aunque la app esté cerrada";textSize=14f;setTextColor(0xff6b7280.toInt());setPadding(0,4,0,18)};root.addView(sub)
        val add=Button(this).apply{text="＋  Nuevo recordatorio";setOnClickListener{editor(null)}};root.addView(add,LinearLayout.LayoutParams(-1,-2))
        val settings=Button(this).apply{text="⚙ Permiso de alarmas exactas";setOnClickListener{AlarmScheduler.requestExactAccess(this@MainActivity)}};root.addView(settings)
        val scroll=ScrollView(this); container=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,12,0,0)};scroll.addView(container);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root);render()
    }
    private fun render(){container.removeAllViews(); if(list.isEmpty()){container.addView(TextView(this).apply{text="No tienes recordatorios todavía.\nPulsa “Nuevo recordatorio” para crear uno.";textSize=17f;setTextColor(0xff6b7280.toInt());setPadding(8,30,8,8)}) ;return}
        list.sortedBy{it.timeMillis}.forEach{r-> val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,16,18,16);setBackgroundColor(0xfff3f4f6.toInt())}
            val title=TextView(this).apply{text="⏰ ${r.title}";textSize=19f;setTypeface(null,Typeface.BOLD);setTextColor(0xff111827.toInt())};card.addView(title)
            val date=TextView(this).apply{text=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(r.timeMillis))+"  •  "+repeatText(r.repeat);textSize=14f;setPadding(0,6,0,4)};card.addView(date)
            if(r.notes.isNotBlank()) card.addView(TextView(this).apply{text=r.notes;textSize=14f})
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            val edit=Button(this).apply{text="Editar";setOnClickListener{editor(r)}}; val del=Button(this).apply{text="Eliminar";setOnClickListener{confirmDelete(r)}}; row.addView(edit,LinearLayout.LayoutParams(0,-2,1f));row.addView(del,LinearLayout.LayoutParams(0,-2,1f));card.addView(row)
            val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,16);container.addView(card,lp)
        }
    }
    private fun repeatText(s:String)=when(s){"daily"->"Cada día";"weekly"->"Cada semana";"monthly"->"Cada mes";else->"Una vez"}
    private fun editor(existing:Reminder?){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(35,10,35,5)}
        val title=EditText(this).apply{hint="Título del recordatorio";setSingleLine();setText(existing?.title?:"")};box.addView(title)
        val notes=EditText(this).apply{hint="Nota (opcional)";setMinLines(2);setText(existing?.notes?:"")};box.addView(notes)
        var cal=Calendar.getInstance(); if(existing!=null) cal.timeInMillis=existing.timeMillis
        val dateBtn=Button(this).apply{text="Fecha: ${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.YEAR)}"};box.addView(dateBtn)
        val timeBtn=Button(this).apply{text=String.format("Hora: %02d:%02d",cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE))};box.addView(timeBtn)
        val reps=arrayOf("Una vez","Cada día","Cada semana","Cada mes"); val repVals=arrayOf("none","daily","weekly","monthly"); val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,reps);if(existing!=null)sp.setSelection(repVals.indexOf(existing.repeat).coerceAtLeast(0));box.addView(sp)
        dateBtn.setOnClickListener{DatePickerDialog(this,{_,y,m,d->cal.set(y,m,d);dateBtn.text="Fecha: $d/${m+1}/$y"},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show()}
        timeBtn.setOnClickListener{TimePickerDialog(this,{_,h,m->cal.set(Calendar.HOUR_OF_DAY,h);cal.set(Calendar.MINUTE,m);cal.set(Calendar.SECOND,0);timeBtn.text=String.format("Hora: %02d:%02d",h,m)},cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()}
        AlertDialog.Builder(this).setTitle(if(existing==null)"Nuevo recordatorio" else "Editar recordatorio").setView(box).setPositiveButton("Guardar"){_,_->
            if(title.text.toString().trim().isEmpty()){Toast.makeText(this,"Escribe un título",Toast.LENGTH_SHORT).show();return@setPositiveButton}
            var t=cal.timeInMillis; if(sp.selectedItemPosition>0 && t<=System.currentTimeMillis()){val tmp=Reminder(existing?.id?:System.currentTimeMillis(),title.text.toString(),notes.text.toString(),t,repVals[sp.selectedItemPosition]);t=AlarmScheduler.nextTrigger(tmp)}
            val r=Reminder(existing?.id?:System.currentTimeMillis(),title.text.toString().trim(),notes.text.toString().trim(),t,repVals[sp.selectedItemPosition],true)
            existing?.let{AlarmScheduler.cancel(this,it.id);list.removeAll{it.id==r.id}};list.add(r);ReminderStore.save(this,list); if(!AlarmScheduler.exactAllowed(this)){AlarmScheduler.requestExactAccess(this)} else AlarmScheduler.schedule(this,r);render();Toast.makeText(this,"Recordatorio guardado",Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancelar",null).show()
    }
    private fun confirmDelete(r:Reminder){AlertDialog.Builder(this).setTitle("Eliminar recordatorio").setMessage("¿Eliminar “${r.title}”? ").setPositiveButton("Eliminar"){_,_->AlarmScheduler.cancel(this,r.id);list.removeAll{it.id==r.id};ReminderStore.save(this,list);render()}.setNegativeButton("Cancelar",null).show()}
}
