package com.example.whatsappscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Bir ScheduledMessage için sistem alarmı kurar / iptal eder.
 */
object AlarmScheduler {

    fun schedule(context: Context, message: ScheduledMessage) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("id", message.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Kullanıcı "tam zamanlı alarm" iznini vermemiş; MainActivity bu izni ayrıca ister.
            // Yine de setAndAllowWhileIdle ile en yakın zamana yakın bir tetikleme dener.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                message.timestampMillis,
                pendingIntent
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            message.timestampMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context, messageId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
