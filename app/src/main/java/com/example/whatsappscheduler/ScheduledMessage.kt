package com.example.whatsappscheduler

/**
 * Zamanlanmış tek bir mesajı temsil eder.
 *
 * @param id Benzersiz kimlik (AlarmManager requestCode olarak da kullanılır)
 * @param phoneNumber Ülke koduyla birlikte, başında + olmadan (ör: 905551234567)
 * @param message Gönderilecek metin
 * @param timestampMillis Gönderimin planlandığı zaman (epoch millis)
 * @param sent Mesaj daha önce tetiklendi mi
 */
data class ScheduledMessage(
    val id: Long,
    val phoneNumber: String,
    val message: String,
    val timestampMillis: Long,
    var sent: Boolean = false
)
