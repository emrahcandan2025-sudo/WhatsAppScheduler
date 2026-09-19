package com.example.whatsappscheduler

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * WhatsApp ekranında bu uygulamanın zamanladığı mesaj için gereken tıklamaları
 * otomatik yapar. İki farklı akışı yönetir:
 *
 * 1) DÜZ METİN: wa.me linkiyle doğru sohbet zaten açılmış olur, tek yapılacak
 *    "gönder" butonuna basmaktır (candidateSendIds).
 *
 * 2) GÖRSEL: WhatsApp'ın "kime iletilsin" ekranı açılır. Önce hedef kişi/numara
 *    metnini ekranda arayıp tıklar (adım 1), ardından açılan onay ekranındaki
 *    gönder/ilet butonunu bulup tıklar (adım 2). Bu akış, WhatsApp arayüz
 *    güncellemelerine metin akışından daha duyarlıdır.
 *
 * NOT: Kullanılan resource-id'ler WhatsApp'ın mevcut sürümlerine göredir;
 * bir güncellemeyle değişirse ilgili candidate listesine yeni id eklemeniz yeterlidir.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var attemptCount = 0
    private val maxAttempts = 15 // ~15 * 400ms = 6 saniye boyunca dener

    private val candidateSendIds = listOf(
        "com.whatsapp:id/send",
        "com.whatsapp.w4b:id/send"
    )

    // Görsel ileti onay ekranındaki "gönder/ilet" butonu için olası id'ler.
    private val candidateForwardConfirmIds = listOf(
        "com.whatsapp:id/fab",
        "com.whatsapp:id/send_button",
        "com.whatsapp.w4b:id/fab"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val storage = MessageStorage(this)

        val pendingId = storage.getPendingSendId()
        if (pendingId != -1L) {
            attemptCount = 0
            tryClickSendButton(storage, pendingId)
            return
        }

        val imageStep = storage.getPendingImageStep()
        if (imageStep == 1) {
            attemptCount = 0
            tryClickContactTarget(storage)
        } else if (imageStep == 2) {
            attemptCount = 0
            tryClickForwardConfirm(storage)
        }
    }

    // --- Adım: düz metin gönderimi ---

    private fun tryClickSendButton(storage: MessageStorage, pendingId: Long) {
        val root = rootInActiveWindow
        if (root != null) {
            val sendButton = findByIds(root, candidateSendIds)
            if (sendButton != null) {
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                storage.markSent(pendingId)
                storage.clearPendingSendId()
                return
            }
        }
        attemptCount++
        if (attemptCount < maxAttempts) {
            handler.postDelayed({ tryClickSendButton(storage, pendingId) }, 400)
        } else {
            storage.clearPendingSendId()
        }
    }

    // --- Adım 1: görsel gönderiminde hedef kişiyi bulup tıkla ---

    private fun tryClickContactTarget(storage: MessageStorage) {
        val target = storage.getPendingImageTarget()
        val root = rootInActiveWindow
        if (target != null && root != null) {
            val node = findClickableNodeByText(root, target)
            if (node != null) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                storage.advancePendingImageStep()
                attemptCount = 0
                // Bir sonraki ekran (onay ekranı) için kısa süre sonra dene.
                handler.postDelayed({ tryClickForwardConfirm(storage) }, 700)
                return
            }
        }
        attemptCount++
        if (attemptCount < maxAttempts) {
            handler.postDelayed({ tryClickContactTarget(storage) }, 400)
        } else {
            // Hedef kişi bulunamadı; kullanıcı elle seçmek zorunda kalacak.
            storage.clearPendingImageJob()
        }
    }

    // --- Adım 2: görsel gönderiminde onay/gönder butonuna bas ---

    private fun tryClickForwardConfirm(storage: MessageStorage) {
        val root = rootInActiveWindow
        if (root != null) {
            val button = findByIds(root, candidateForwardConfirmIds)
                ?: findClickableNodeByContentDescription(root, listOf("Gönder", "Send", "İlet", "Forward"))
            if (button != null) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val messageId = storage.getPendingImageMessageId()
                if (messageId != -1L) storage.markSent(messageId)
                storage.clearPendingImageJob()
                return
            }
        }
        attemptCount++
        if (attemptCount < maxAttempts) {
            handler.postDelayed({ tryClickForwardConfirm(storage) }, 400)
        } else {
            storage.clearPendingImageJob()
        }
    }

    // --- Yardımcı arama fonksiyonları ---

    private fun findByIds(node: AccessibilityNodeInfo, ids: List<String>): AccessibilityNodeInfo? {
        for (id in ids) {
            val matches = node.findAccessibilityNodeInfosByViewId(id)
            if (matches.isNotEmpty()) return matches[0]
        }
        return null
    }

    /** Verilen metni (tam veya içeren, büyük/küçük harf duyarsız) taşıyan,
     * tıklanabilir bir düğüm ya da tıklanabilir bir üst öğesi olan düğüm arar. */
    private fun findClickableNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val matches = root.findAccessibilityNodeInfosByText(text)
        for (match in matches) {
            val clickable = nearestClickableAncestor(match)
            if (clickable != null) return clickable
        }
        return null
    }

    private fun findClickableNodeByContentDescription(
        root: AccessibilityNodeInfo,
        candidates: List<String>
    ): AccessibilityNodeInfo? {
        for (candidate in candidates) {
            val matches = root.findAccessibilityNodeInfosByText(candidate)
            for (match in matches) {
                val clickable = nearestClickableAncestor(match)
                if (clickable != null) return clickable
            }
        }
        return null
    }

    private fun nearestClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    override fun onInterrupt() {
        // Gerekli değil, boş bırakılabilir.
    }
}
