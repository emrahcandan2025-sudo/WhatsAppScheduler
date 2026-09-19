package com.example.whatsappscheduler

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsappscheduler.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: MessageStorage
    private lateinit var adapter: ScheduledMessagesAdapter

    private val selectedCalendar = Calendar.getInstance()
    private var dateChosen = false
    private var timeChosen = false

    private var selectedContactName: String? = null
    private var selectedImageFileName: String? = null

    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            readContact(contactUri)
        }
    }

    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchContactPicker()
        else Toast.makeText(this, "Rehber izni verilmedi", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = ImageStore.copyFrom(this, uri)
            if (fileName != null) {
                selectedImageFileName = fileName
                showImagePreview(fileName)
            } else {
                Toast.makeText(this, "Görsel kopyalanamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = MessageStorage(this)

        setupRecyclerView()
        setupListeners()
        requestExactAlarmPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = ScheduledMessagesAdapter(mutableListOf()) { message ->
            AlarmScheduler.cancel(this, message.id)
            message.imageFileName?.let { ImageStore.delete(this, it) }
            storage.remove(message.id)
            refreshList()
        }
        binding.recyclerScheduledMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerScheduledMessages.adapter = adapter
    }

    private fun refreshList() {
        adapter.updateData(storage.getAll())
    }

    private fun setupListeners() {
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnPickTime.setOnClickListener { showTimePicker() }
        binding.btnSchedule.setOnClickListener { scheduleMessage() }
        binding.btnPickContact.setOnClickListener { checkContactsPermissionAndPick() }
        binding.btnPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnRemoveImage.setOnClickListener { clearSelectedImage() }
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(
                this,
                "Listeden \"CNDN WP\" servisini bulup açın.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- Rehberden kişi seçme ---

    private fun checkContactsPermissionAndPick() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            launchContactPicker()
        } else {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun readContact(contactUri: Uri) {
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                val rawNumber = if (numberIndex != -1) it.getString(numberIndex) else null
                val name = if (nameIndex != -1) it.getString(nameIndex) else null

                if (rawNumber != null) {
                    val normalized = rawNumber.replace(Regex("[^0-9]"), "")
                    binding.editPhoneNumber.setText(normalized)
                }
                selectedContactName = name
            }
        }
    }

    // --- Görsel seçme ---

    private fun showImagePreview(fileName: String) {
        binding.imagePreview.setImageURI(Uri.fromFile(ImageStore.fileFor(this, fileName)))
        binding.imagePreview.visibility = android.view.View.VISIBLE
        binding.btnRemoveImage.visibility = android.view.View.VISIBLE
    }

    private fun clearSelectedImage() {
        selectedImageFileName?.let { ImageStore.delete(this, it) }
        selectedImageFileName = null
        binding.imagePreview.setImageDrawable(null)
        binding.imagePreview.visibility = android.view.View.GONE
        binding.btnRemoveImage.visibility = android.view.View.GONE
    }

    // --- Tarih / saat seçimi ---

    private fun showDatePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateChosen = true
                updateSelectedDateTimeLabel()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                timeChosen = true
                updateSelectedDateTimeLabel()
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateSelectedDateTimeLabel() {
        if (dateChosen && timeChosen) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("tr"))
            binding.textSelectedDateTime.text = "Seçilen zaman: ${sdf.format(selectedCalendar.time)}"
        }
    }

    // --- Zamanlama ---

    private fun scheduleMessage() {
        val phone = binding.editPhoneNumber.text?.toString()?.trim()?.replace("+", "")?.replace(" ", "")
        val text = binding.editMessage.text?.toString()?.trim()

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Telefon numarası girin (ör: 905551234567)", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(text) && selectedImageFileName == null) {
            Toast.makeText(this, "Mesaj metni girin veya görsel ekleyin", Toast.LENGTH_SHORT).show()
            return
        }
        if (!dateChosen || !timeChosen) {
            Toast.makeText(this, "Tarih ve saat seçin", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Seçilen zaman geçmişte kalıyor, ileri bir zaman seçin", Toast.LENGTH_SHORT).show()
            return
        }

        val message = ScheduledMessage(
            id = System.currentTimeMillis(),
            phoneNumber = phone!!,
            contactName = selectedContactName,
            message = text ?: "",
            imageFileName = selectedImageFileName,
            timestampMillis = selectedCalendar.timeInMillis
        )

        storage.add(message)
        AlarmScheduler.schedule(this, message)
        refreshList()

        Toast.makeText(this, "Mesaj zamanlandı", Toast.LENGTH_SHORT).show()

        binding.editPhoneNumber.text?.clear()
        binding.editMessage.text?.clear()
        selectedContactName = null
        selectedImageFileName = null
        binding.imagePreview.setImageDrawable(null)
        binding.imagePreview.visibility = android.view.View.GONE
        binding.btnRemoveImage.visibility = android.view.View.GONE
        dateChosen = false
        timeChosen = false
        binding.textSelectedDateTime.text = "Seçilen zaman: -"
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Mesajların tam zamanında gönderilmesi için izni açın.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
