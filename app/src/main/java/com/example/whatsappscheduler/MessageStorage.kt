package com.example.whatsappscheduler

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Zamanlanmış mesajları basitçe SharedPreferences içinde JSON olarak saklar.
 * Küçük ölçekli kişisel kullanım için yeterlidir; büyük hacimde mesaj
 * planlamak isterseniz bunun yerine bir Room veritabanı kullanmanız önerilir.
 */
class MessageStorage(context: Context) {

    private val prefs = context.getSharedPreferences("whatsapp_scheduler_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MESSAGES = "scheduled_messages"
        private const val KEY_PENDING_SEND_ID = "pending_send_id"
        private const val KEY_PENDING_IMAGE_STEP = "pending_image_step"
        private const val KEY_PENDING_IMAGE_TARGET = "pending_image_target"
        private const val KEY_PENDING_IMAGE_MESSAGE_ID = "pending_image_message_id"
    }

    fun getAll(): MutableList<ScheduledMessage> {
        val json = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<ScheduledMessage>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ScheduledMessage(
                    id = obj.getLong("id"),
                    phoneNumber = obj.getString("phoneNumber"),
                    contactName = if (obj.has("contactName") && !obj.isNull("contactName")) obj.getString("contactName") else null,
                    message = obj.getString("message"),
                    imageFileName = if (obj.has("imageFileName") && !obj.isNull("imageFileName")) obj.getString("imageFileName") else null,
                    timestampMillis = obj.getLong("timestampMillis"),
                    sent = obj.optBoolean("sent", false)
                )
            )
        }
        return list
    }

    private fun saveAll(list: List<ScheduledMessage>) {
        val array = JSONArray()
        for (m in list) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("phoneNumber", m.phoneNumber)
            obj.put("contactName", m.contactName)
            obj.put("message", m.message)
            obj.put("imageFileName", m.imageFileName)
            obj.put("timestampMillis", m.timestampMillis)
            obj.put("sent", m.sent)
            array.put(obj)
        }
        prefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    fun add(message: ScheduledMessage) {
        val list = getAll()
        list.add(message)
        saveAll(list)
    }

    fun remove(id: Long) {
        val list = getAll().filterNot { it.id == id }
        saveAll(list)
    }

    fun markSent(id: Long) {
        val list = getAll()
        list.find { it.id == id }?.sent = true
        saveAll(list)
    }

    fun getById(id: Long): ScheduledMessage? = getAll().find { it.id == id }

    /** Henüz gönderilmemiş ve zamanı gelmemiş mesajları döner (reboot sonrası yeniden kurmak için). */
    fun getPendingFuture(): List<ScheduledMessage> =
        getAll().filter { !it.sent && it.timestampMillis > System.currentTimeMillis() }

    // --- Düz metin gönderimi için bekleme durumu (mevcut) ---

    fun setPendingSendId(id: Long) {
        prefs.edit().putLong(KEY_PENDING_SEND_ID, id).apply()
    }

    fun getPendingSendId(): Long = prefs.getLong(KEY_PENDING_SEND_ID, -1L)

    fun clearPendingSendId() {
        prefs.edit().putLong(KEY_PENDING_SEND_ID, -1L).apply()
    }

    // --- Görsel gönderimi için çok adımlı bekleme durumu ---
    // step 0 = beklenen yok, 1 = "kime iletilsin" ekranında kişi seçimi bekleniyor,
    // 2 = kişi seçildi, gönder/onayla butonunun tıklanması bekleniyor

    fun setPendingImageJob(messageId: Long, target: String) {
        prefs.edit()
            .putInt(KEY_PENDING_IMAGE_STEP, 1)
            .putString(KEY_PENDING_IMAGE_TARGET, target)
            .putLong(KEY_PENDING_IMAGE_MESSAGE_ID, messageId)
            .apply()
    }

    fun getPendingImageStep(): Int = prefs.getInt(KEY_PENDING_IMAGE_STEP, 0)

    fun getPendingImageTarget(): String? = prefs.getString(KEY_PENDING_IMAGE_TARGET, null)

    fun getPendingImageMessageId(): Long = prefs.getLong(KEY_PENDING_IMAGE_MESSAGE_ID, -1L)

    fun advancePendingImageStep() {
        prefs.edit().putInt(KEY_PENDING_IMAGE_STEP, 2).apply()
    }

    fun clearPendingImageJob() {
        prefs.edit()
            .putInt(KEY_PENDING_IMAGE_STEP, 0)
            .remove(KEY_PENDING_IMAGE_TARGET)
            .remove(KEY_PENDING_IMAGE_MESSAGE_ID)
            .apply()
    }
}
