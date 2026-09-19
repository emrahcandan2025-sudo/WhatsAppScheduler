package com.example.whatsappscheduler

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(
                this,
                "Listeden \"WhatsApp Zamanlayıcı\" servisini bulup açın.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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

    private fun scheduleMessage() {
        val phone = binding.editPhoneNumber.text?.toString()?.trim()?.replace("+", "")?.replace(" ", "")
        val text = binding.editMessage.text?.toString()?.trim()

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Telefon numarası girin (ör: 905551234567)", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Mesaj metni girin", Toast.LENGTH_SHORT).show()
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
            message = text!!,
            timestampMillis = selectedCalendar.timeInMillis
        )

        storage.add(message)
        AlarmScheduler.schedule(this, message)
        refreshList()

        Toast.makeText(this, "Mesaj zamanlandı", Toast.LENGTH_SHORT).show()

        binding.editPhoneNumber.text?.clear()
        binding.editMessage.text?.clear()
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
