package com.example.whatsappscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Zamanlanan saat geldiğinde tetiklenir. WhatsApp'ı, numarası ve mesajı
 * hazır dolu şekilde açar. Gerçek "gönder" tıklamasını WhatsAppAutoSendService
 * (Erişilebilirlik servisi) yapar; bu receiver sadece ekranı hazırlar.
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

        val encodedText = URLEncoder.encode(message.message, "UTF-8")
        val uri = Uri.parse("https://wa.me/${message.phoneNumber}?text=$encodedText")

        val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // WhatsApp yüklü değilse veya paket adı farklıysa (ör. WhatsApp Business),
            // paket kısıtlaması olmadan tekrar dene.
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }
}
