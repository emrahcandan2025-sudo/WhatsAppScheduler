package com.example.whatsappscheduler

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager

/**
 * Görünmez "köprü" Activity.
 *
 * AlarmReceiver'ın gösterdiği tam ekran bildirimi bu Activity'yi ekran
 * kapalı/kilitli olsa bile öne getirir (full-screen intent, sistem tarafından
 * arka plan kısıtlamasından muaf tutulur). Activity foreground'a geldiği anda
 * WhatsApp'ı açan gerçek Intent'i tetikleriz — bu noktada uygulamanın görünür
 * bir ekranı olduğu için Android'in arka plan startActivity() kısıtlaması
 * artık geçerli değildir.
 */
class WhatsAppLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kilit ekranının üstünde göster ve ekranı aç.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val id = intent.getLongExtra("id", -1L)

        // Tetikleyen bildirimi kapat.
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (id != -1L) {
            notificationManager.cancel(id.toInt())
        }

        if (id != -1L) {
            val storage = MessageStorage(this)
            val message = storage.getById(id)
            if (message != null && !message.sent) {
                // Artık foreground bir Activity içindeyiz: startActivity() burada
                // arka plan kısıtlamasına takılmaz.
                WhatsAppSender.send(this, storage, message)
            }
        }

        // Görsel bir arayüz göstermiyoruz; işini bitirince hemen kapan.
        finish()
        overridePendingTransition(0, 0)
    }
}
