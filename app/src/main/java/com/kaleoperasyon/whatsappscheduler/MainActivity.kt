package com.kaleoperasyon.whatsappscheduler

import android.app.*
import android.content.*
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var phone: EditText
    private lateinit var message: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private val selectedDate = Calendar.getInstance()
    private var hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var minute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,50,40,40) }
        val title = TextView(this).apply { text="WhatsApp Zamanlayıcı"; textSize=26f }
        phone = EditText(this).apply { hint="Telefon numarası (905xxxxxxxxx)"; inputType=3 }
        message = EditText(this).apply { hint="Gönderilecek mesaj"; minLines=4; gravity=48 }
        dateButton = Button(this).apply { setOnClickListener { chooseDate() } }
        timeButton = Button(this).apply { setOnClickListener { chooseTime() } }
        val schedule = Button(this).apply { text="MESAJI ZAMANLA"; setOnClickListener { scheduleMessage() } }
        layout.addView(title,lp()); layout.addView(phone,lp()); layout.addView(message,lp()); layout.addView(dateButton,lp()); layout.addView(timeButton,lp()); layout.addView(schedule,lp())
        setContentView(layout); updateButtons()
    }
    private fun lp()=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(0,10,0,10)}
    private fun chooseDate(){ val c=Calendar.getInstance(); DatePickerDialog(this,{_,y,m,d->selectedDate.set(y,m,d);updateButtons()},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show() }
    private fun chooseTime(){ TimePickerDialog(this,{_,h,m->hour=h;minute=m;updateButtons()},hour,minute,true).show() }
    private fun updateButtons(){ dateButton.text="Tarih: "+SimpleDateFormat("dd.MM.yyyy",Locale("tr")).format(selectedDate.time); timeButton.text=String.format(Locale("tr"),"Saat: %02d:%02d",hour,minute) }
    private fun scheduleMessage(){
        val number=phone.text.toString().trim(); val text=message.text.toString().trim()
        if(number.isEmpty()||text.isEmpty()){Toast.makeText(this,"Telefon numarası ve mesaj gerekli.",Toast.LENGTH_LONG).show();return}
        val cal=Calendar.getInstance().apply{set(selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH),hour,minute,0)}
        if(cal.timeInMillis<=System.currentTimeMillis()){Toast.makeText(this,"Geçmiş bir tarih/saat seçilemez.",Toast.LENGTH_LONG).show();return}
        val intent=Intent(this,ScheduleReceiver::class.java).apply{putExtra("phone",number);putExtra("message",text)}
        val id=(System.currentTimeMillis() and 0xfffffff).toInt()
        val pending=PendingIntent.getBroadcast(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm=getSystemService(ALARM_SERVICE) as AlarmManager
        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,pending)
        Toast.makeText(this,"Mesaj zamanlandı.",Toast.LENGTH_LONG).show(); phone.text.clear(); message.text.clear()
    }
}
