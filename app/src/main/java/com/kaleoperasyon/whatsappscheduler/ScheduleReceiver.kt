package com.kaleoperasyon.whatsappscheduler

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat

class ScheduleReceiver: BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val phone=intent.getStringExtra("phone") ?: ""; val message=intent.getStringExtra("message") ?: ""
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; val channel="scheduled_messages"
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channel,"Zamanlanmış Mesajlar",NotificationManager.IMPORTANCE_HIGH))
        val notification=NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("WhatsApp mesajı zamanı geldi").setContentText(phone).setStyle(NotificationCompat.BigTextStyle().bigText(message)).setAutoCancel(true).build()
        manager.notify((System.currentTimeMillis() and 0xfffffff).toInt(),notification)
    }
}
