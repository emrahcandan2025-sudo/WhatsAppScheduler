package com.example.whatsappscheduler

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Kullanıcının galeriden seçtiği görselleri uygulamanın kendi özel
 * depolama alanına kopyalar. Böylece görsel, telefon yeniden başlasa
 * veya galeriden silinse bile gönderim anında hâlâ erişilebilir olur.
 */
object ImageStore {

    private const val FOLDER_NAME = "scheduled_images"

    private fun folder(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Verilen content Uri'sindeki görseli kopyalar, saklanan dosya adını döner. */
    fun copyFrom(context: Context, sourceUri: Uri): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(folder(context), fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    fun fileFor(context: Context, fileName: String): File = File(folder(context), fileName)

    /** WhatsApp'a paylaşabilmek için FileProvider üzerinden content:// Uri üretir. */
    fun contentUriFor(context: Context, fileName: String): Uri {
        val file = fileFor(context, fileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun delete(context: Context, fileName: String) {
        fileFor(context, fileName).delete()
    }
}
