package com.example.whatsappscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Zamanlanan saat geldiğinde tetiklenir. Ekranı uyandırıp WhatsApp'ı
 * mesaj hazır şekilde açan WakeUpActivity'yi başlatır. Gerçek "gönder"
 * tıklamasını WhatsAppAutoSendService (Erişilebilirlik servisi) yapar;
 * bu receiver sadece süreci başlatır.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) return

        val storage = MessageStorage(context)
        val message = storage.getById(id) ?: return
        if (message.sent) return

        // Erişilebilirlik servisine "bu mesaj için gönderim bekleniyor" bilgisini bırak.
        storage.setPendingSendId(id)

        val wakeUpIntent = Intent(context, WakeUpActivity::class.java).apply {
            putExtra("phone", message.phoneNumber)
            putExtra("message", message.message)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        context.startActivity(wakeUpIntent)
    }
}
