package com.example.whatsappscheduler

import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

/**
 * Ekran kapalıyken alarm tetiklendiğinde devreye giren, kullanıcıya görünmeyen
 * (saydam) bir aktivite. Görevi:
 *  1) Ekranı gerçekten uyandırmak (aksi halde WhatsApp arka planda "sessizce"
 *     açılır ama render edilmediği için Erişilebilirlik servisi butonu bulamaz)
 *  2) Kilit ekranını aşmaya çalışmak (telefonda PIN/desen/parmak izi gibi
 *     güvenli bir kilit varsa, Android güvenlik gereği bunu otomatik geçemeyiz;
 *     kullanıcının elle kilidi açması gerekir — bu durumda mesaj kilit ekranının
 *     arkasında bekler, kilit açılınca akış devam eder)
 *  3) Ardından WhatsApp'ı mesaj hazır şekilde açmak
 */
class WakeUpActivity : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "WhatsAppScheduler:wakeup"
        )
        wakeLock?.acquire(15_000L)

        val phone = intent.getStringExtra("phone")
        val message = intent.getStringExtra("message")

        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    openWhatsApp(phone, message)
                }

                override fun onDismissError() {
                    // Güvenli kilit (PIN/desen/biyometrik) açık; kullanıcı elle
                    // kilidi açmadan devam edemeyiz. Yine de denemeyi bırakmıyoruz,
                    // çünkü bazı cihazlarda swipe-only kilitte bu yol çalışır.
                    openWhatsApp(phone, message)
                }

                override fun onDismissCancelled() {
                    finish()
                }
            })
        } else {
            openWhatsApp(phone, message)
        }
    }

    private fun openWhatsApp(phone: String?, message: String?) {
        if (phone.isNullOrEmpty() || message.isNullOrEmpty()) {
            finish()
            return
        }

        val encodedText = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$phone?text=$encodedText")

        val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(whatsappIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(fallbackIntent)
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
