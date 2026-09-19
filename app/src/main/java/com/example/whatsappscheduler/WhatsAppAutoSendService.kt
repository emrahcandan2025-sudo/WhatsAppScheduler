package com.example.whatsappscheduler

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * WhatsApp ekranında, bu uygulamanın az önce açtığı sohbette "gönder"
 * butonunu bulup otomatik tıklar. Sadece MessageStorage'da bir
 * "pendingSendId" varken çalışır; yani sadece bu uygulamanın kendi
 * zamanladığı mesajlar için devreye girer, başka hiçbir WhatsApp
 * etkileşimine karışmaz.
 *
 * NOT: "com.whatsapp:id/send" WhatsApp'ın mevcut sürümlerinde gönder
 * butonunun tipik resource-id'sidir. WhatsApp arayüzü güncellendiğinde bu
 * id değişebilir; öyle bir durumda aşağıdaki CANDIDATE_SEND_IDS listesine
 * yeni id'yi eklemeniz yeterlidir.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var attemptCount = 0
    private val maxAttempts = 15 // ~15 * 400ms = 6 saniye boyunca dener

    private val candidateSendIds = listOf(
        "com.whatsapp:id/send",
        "com.whatsapp.w4b:id/send"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val storage = MessageStorage(this)
        val pendingId = storage.getPendingSendId()
        if (pendingId == -1L) return // Bu uygulamanın beklediği bir gönderim yok, hiçbir şey yapma.

        attemptCount = 0
        tryClickSendButton(storage, pendingId)
    }

    private fun tryClickSendButton(storage: MessageStorage, pendingId: Long) {
        val root = rootInActiveWindow
        if (root != null) {
            val sendButton = findSendButton(root)
            if (sendButton != null) {
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                storage.markSent(pendingId)
                storage.clearPendingSendId()
                return
            }
        }

        attemptCount++
        if (attemptCount < maxAttempts) {
            // WhatsApp ekranı henüz tam yüklenmemiş olabilir, kısa süre sonra tekrar dene.
            handler.postDelayed({ tryClickSendButton(storage, pendingId) }, 400)
        } else {
            // Buton bulunamadı (metin kutusu boş kalmış olabilir, ya da id değişmiş olabilir).
            // Sonsuz denemeyi önlemek için pes ediyoruz; kullanıcı mesajı elle gönderebilir.
            storage.clearPendingSendId()
        }
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in candidateSendIds) {
            val matches = node.findAccessibilityNodeInfosByViewId(id)
            if (matches.isNotEmpty()) {
                return matches[0]
            }
        }
        return null
    }

    override fun onInterrupt() {
        // Gerekli değil, boş bırakılabilir.
    }
}
