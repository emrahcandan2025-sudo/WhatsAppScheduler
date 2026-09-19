package com.example.whatsappscheduler

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Bir ScheduledMessage'ı WhatsApp'a göndermek için gerekli Intent'i hazırlar
 * ve Erişilebilirlik servisine hangi bekleme durumuna geçmesi gerektiğini
 * (MessageStorage üzerinden) haber verir.
 *
 * - Görsel YOKSA: wa.me linkiyle doğrudan doğru sohbet açılır, mesaj hazır gelir.
 *   (WhatsAppAutoSendService bu durumda tek adımda "gönder" butonuna basar.)
 * - Görsel VARSA: WhatsApp'ın kendi "kime iletilsin" ekranı açılır, çünkü WhatsApp
 *   görsel için doğrudan sohbet linkine izin vermiyor. Bu ekranda doğru kişiyi
 *   bulup tıklamak ve ardından gönderme onayını vermek gerekir — bu, metin
 *   gönderimine göre daha kırılgan bir otomasyondur (WhatsApp arayüz güncellemesi
 *   bu akışı metne göre daha kolay bozabilir).
 */
object WhatsAppSender {

    fun send(context: Context, storage: MessageStorage, message: ScheduledMessage) {
        val imageFileName = message.imageFileName
        if (imageFileName.isNullOrEmpty()) {
            sendTextOnly(context, storage, message)
        } else {
            sendWithImage(context, storage, message, imageFileName)
        }
    }

    private fun sendTextOnly(context: Context, storage: MessageStorage, message: ScheduledMessage) {
        storage.setPendingSendId(message.id)

        val encodedText = URLEncoder.encode(message.message, "UTF-8")
        val uri = Uri.parse("https://wa.me/${message.phoneNumber}?text=$encodedText")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun sendWithImage(
        context: Context,
        storage: MessageStorage,
        message: ScheduledMessage,
        imageFileName: String
    ) {
        // Eşleştirme hedefi: rehberden seçilmişse kişi adı, değilse telefon numarası.
        val target = message.contactName?.takeIf { it.isNotBlank() } ?: message.phoneNumber
        storage.setPendingImageJob(message.id, target)

        val imageUri = ImageStore.contentUriFor(context, imageFileName)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            if (message.message.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, message.message)
            }
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
