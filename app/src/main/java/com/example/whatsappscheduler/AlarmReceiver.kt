package com.example.whatsappscheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Zamanlanan saat geldiğinde tetiklenir.
 *
 * ÖNEMLİ: Android 10+ (API 29+), arka plandaki bir bileşenden (BroadcastReceiver,
 * Service) doğrudan startActivity() çağırmayı engeller — ekranı uyandırmak bile
 * bu kısıtlamayı kaldırmaz. Bu yüzden burada WhatsApp'ı DOĞRUDAN açmaya
 * çalışmıyoruz; bunun yerine tam ekran bildirim (full-screen intent) gösteriyoruz.
 * Bu, alarm ve çağrı uygulamalarının kullandığı, sistem tarafından arka plan
 * kısıtlamasından muaf tutulan tek güvenilir yöntemdir. Bildirim,
 * WhatsAppLaunchActivity'yi ekran kapalı/kilitli olsa bile öne getirir; o
 * Activity göründüğü an (artık foreground) gerçek WhatsApp Intent'ini tetikler.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "whatsapp_scheduler_alarm"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) return

        val storage = MessageStorage(context)
        val message = storage.getById(id) ?: return
        if (message.sent) return

        showFullScreenLaunch(context, id)
    }

    private fun showFullScreenLaunch(context: Context, id: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesaj gönderimi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Zamanlanan WhatsApp mesajı gönderilirken gösterilir"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, WhatsAppLaunchActivity::class.java).apply {
            putExtra("id", id)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            )
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email) // kendi ikonunuzla değiştirin
            .setContentTitle("WhatsApp mesajı gönderiliyor")
            .setContentText(message@ run {
                val storage = MessageStorage(context)
                storage.getById(id)?.phoneNumber?.let { "$it numarasına gönderiliyor" }
                    ?: "Gönderiliyor..."
            })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.toInt(), notification)
    }
}
