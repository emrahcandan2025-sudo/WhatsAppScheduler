package com.example.whatsappscheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Telefon kilitliyken tetiklenen zamanlanmış mesajlar için devreye girer.
 * Kullanıcı telefonun kilidini AÇTIĞI anda (ACTION_USER_PRESENT) mesajı
 * (metin veya görsel) gönderir.
 *
 * Android güvenlik gereği hiçbir uygulama gerçek bir PIN/desen/parmak izi
 * kilidini kullanıcı adına otomatik açamaz — bu servis o kilidi açmaya
 * ÇALIŞMAZ, sadece kullanıcı kendi açtığı anda bekleyen mesajı gönderir.
 */
class UnlockWaiterService : Service() {

    private var messageId: Long = -1L
    private val timeoutHandler = Handler(Looper.getMainLooper())

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                sendNow()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        messageId = intent?.getLongExtra("messageId", -1L) ?: -1L

        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))

        // 30 dakika içinde kilit açılmazsa pes et, sonsuza kadar beklemeyelim.
        timeoutHandler.postDelayed({ stopWaiting() }, 30 * 60 * 1000L)

        return START_NOT_STICKY
    }

    private fun sendNow() {
        if (messageId != -1L) {
            val storage = MessageStorage(this)
            val message = storage.getById(messageId)
            if (message != null) {
                WhatsAppSender.send(this, storage, message)
            }
        }
        stopWaiting()
    }

    private fun stopWaiting() {
        timeoutHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // zaten kayıtlı değilse görmezden gel
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val channelId = "unlock_waiter_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Zamanlanmış mesaj bekleniyor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("CNDN WP")
            .setContentText("Kilidi açınca zamanlanmış mesaj gönderilecek")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // zaten kayıtlı değilse görmezden gel
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 4821
    }
}
