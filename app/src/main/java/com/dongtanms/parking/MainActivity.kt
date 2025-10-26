package com.dongtanms.parking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var inputView: TextView
    private lateinit var recyclerView: RecyclerView
    private var input = StringBuilder()
    private lateinit var adapter: ParkingAdapter
    private lateinit var ttsManager: TTSManager
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val autoBackupRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                BackupManager.autoBackup(this@MainActivity)
            }
            handler.postDelayed(this, 5 * 60 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputView = findViewById(R.id.tvInput)
        recyclerView = findViewById(R.id.recyclerParking)

        adapter = ParkingAdapter(
            onDelete = { deleteCar(it) },
            onDone = { markAsDone(it) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.itemAnimator?.apply {
            addDuration = 300
            removeDuration = 300
            changeDuration = 250
        }

        ttsManager = TTSManager(this)
        handler.postDelayed(autoBackupRunnable, 5 * 60 * 1000)

        setupButtons()
        updateInput()
        loadParkingList()

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 100
            )
        }
    }

    private fun setupButtons() {
        val btnIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        btnIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                if (input.length < 4) {
                    input.append((it as Button).text)
                    updateInput()

                    if (input.length == 4) {
                        handler.postDelayed({
                            registerCar(input.toString())
                            input.clear()
                            updateInput()
                        }, 200)
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (input.isNotEmpty()) {
                input.deleteCharAt(input.length - 1)
                updateInput()
            }
        }

        findViewById<ImageButton>(R.id.btnAdmin).setOnClickListener {
            showAdminPasswordDialog()
        }
    }

    private fun showAdminPasswordDialog() {
        val editText = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("관리자 암호")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                if (editText.text.toString() == "8007") {
                    startActivity(Intent(this, AdminActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateInput() {
        if (input.isEmpty()) {
            inputView.text = "차량 번호를 입력해주세요"
            inputView.textSize = 48f
            inputView.setTextColor(ContextCompat.getColor(this, R.color.button_neutral))
        } else {
            inputView.text = input.toString()
            inputView.textSize = 64f
            inputView.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        }
    }

    private fun createGroupedList(entries: List<ParkingEntry>): List<ListItem> {
        val groupedList = mutableListOf<ListItem>()
        if (entries.isEmpty()) return groupedList

        val sdf = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
        var lastHeader = ""

        for (entry in entries) {
            val dateString = sdf.format(Date(entry.createdAt))
            if (dateString != lastHeader) {
                groupedList.add(ListItem.DateItem(dateString))
                lastHeader = dateString
            }
            groupedList.add(ListItem.EntryItem(entry))
        }
        return groupedList
    }

    private fun registerCar(number: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().insert(ParkingEntry(plateNumber = number))
            withContext(Dispatchers.Main) {
                ttsManager.speak("등록되었습니다")
                loadParkingList()
            }
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun deleteCar(entry: ParkingEntry) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().delete(entry)
            withContext(Dispatchers.Main) {
                loadParkingList()
            }
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun markAsDone(entry: ParkingEntry) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().update(entry.copy(status = "done"))
            withContext(Dispatchers.Main) {
                loadParkingList()
            }
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun loadParkingList() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val data = db.parkingDao().getAll()
            val listItems = createGroupedList(data)
            withContext(Dispatchers.Main) {
                adapter.updateData(listItems)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        handler.removeCallbacks(autoBackupRunnable)
        lifecycleScope.launch {
            BackupManager.autoBackup(this@MainActivity)
        }
    }
}
