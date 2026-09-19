package com.example.whatsappscheduler

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

/**
 * Zamanlanan saat geldiğinde tetiklenir.
 *  - Önce ekranı uyandırır (aksi halde WhatsApp arka planda render edilmediği
 *    için Erişilebilirlik servisi gönder butonunu bulamaz).
 *  - Telefon kilitli DEĞİLSE: doğrudan WhatsAppSender ile gönderimi başlatır.
 *  - Telefon güvenli bir kilitle (PIN/desen/parmak izi) KİLİTLİYSE:
 *    UnlockWaiterService'i başlatır; bu servis kullanıcı kilidi AÇTIĞI anda
 *    mesajı gönderir. (Android güvenlik gereği kilidi otomatik açamayız.)
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) return

        val storage = MessageStorage(context)
        val message = storage.getById(id) ?: return
        if (message.sent) return

        wakeScreen(context)

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager.isDeviceLocked
        } else {
            keyguardManager.isKeyguardLocked
        }

        if (isLocked) {
            val serviceIntent = Intent(context, UnlockWaiterService::class.java).apply {
                putExtra("messageId", id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            storage.markSent(id) // tekrar tetiklenmesin; asıl gönderim servis tarafından yapılacak
            return
        }

        WhatsAppSender.send(context, storage, message)
    }

    private fun wakeScreen(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "WhatsAppScheduler:alarmWake"
        )
        wakeLock.acquire(15_000L)
    }
}
