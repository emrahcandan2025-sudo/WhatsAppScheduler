package com.example.whatsappscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Telefon yeniden başlatıldığında, henüz gönderilmemiş ve zamanı gelmemiş
 * mesajlar için alarmları tekrar kurar (AlarmManager alarmları reboot'ta silinir).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val storage = MessageStorage(context)
        storage.getPendingFuture().forEach { message ->
            AlarmScheduler.schedule(context, message)
        }
    }
}
