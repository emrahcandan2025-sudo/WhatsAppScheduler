package com.example.whatsappscheduler

/**
 * Zamanlanmış tek bir mesajı temsil eder.
 *
 * @param id Benzersiz kimlik (AlarmManager requestCode olarak da kullanılır)
 * @param phoneNumber Ülke koduyla birlikte, başında + olmadan (ör: 905551234567)
 * @param contactName Rehberden seçildiyse kişinin görünen adı (görsel gönderiminde
 *        WhatsApp'ın "kime iletilsin" ekranında doğru kişiyi bulmak için kullanılır)
 * @param message Gönderilecek metin (görsel varsa, görselin altyazısı olur)
 * @param imageFileName Uygulamanın kendi deposunda (scheduled_images/ klasörü)
 *        sakladığı görsel dosyasının adı; görsel eklenmediyse null
 * @param timestampMillis Gönderimin planlandığı zaman (epoch millis)
 * @param sent Mesaj daha önce tetiklendi mi
 */
data class ScheduledMessage(
    val id: Long,
    val phoneNumber: String,
    val contactName: String? = null,
    val message: String,
    val imageFileName: String? = null,
    val timestampMillis: Long,
    var sent: Boolean = false
)
