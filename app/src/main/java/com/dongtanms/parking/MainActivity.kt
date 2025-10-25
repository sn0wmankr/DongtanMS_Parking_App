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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
            handler.postDelayed(this, 5 * 60 * 1000) // 5분마다 자동 백업
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputView = findViewById(R.id.tvInput)
        recyclerView = findViewById(R.id.recyclerParking)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ParkingAdapter(
            emptyList(),
            onDelete = { deleteCar(it) },
            onDone = { markAsDone(it) }
        )
        recyclerView.adapter = adapter

        recyclerView.itemAnimator?.apply {
            addDuration = 300
            removeDuration = 300
            changeDuration = 250
        }

        ttsManager = TTSManager(this)
        handler.postDelayed(autoBackupRunnable, 5 * 60 * 1000)

        setupButtons()
        updateInput() // 초기 입력창 상태 설정

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
                    ttsManager.speak("관리자 모드로 전환합니다")
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

    private fun numberToKorean(number: String): String {
        val koreanNumbers = mapOf(
            '0' to "영", '1' to "일", '2' to "이", '3' to "삼", '4' to "사",
            '5' to "오", '6' to "육", '7' to "칠", '8' to "팔", '9' to "구"
        )
        return number.map { koreanNumbers[it] }.joinToString(" ")
    }

    private fun registerCar(number: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().insert(ParkingEntry(plateNumber = number))
            val koreanNumber = numberToKorean(number)
            ttsManager.speak("$koreanNumber 번 차량 등록되었습니다")
            loadParkingList()
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun deleteCar(entry: ParkingEntry) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().delete(entry)
            val koreanNumber = numberToKorean(entry.plateNumber)
            ttsManager.speak("$koreanNumber 번 차량이 삭제되었습니다")
            loadParkingList()
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun markAsDone(entry: ParkingEntry) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().update(entry.copy(status = "done"))
            val koreanNumber = numberToKorean(entry.plateNumber)
            ttsManager.speak("$koreanNumber 번 차량 완료 처리되었습니다")
            loadParkingList()
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun loadParkingList() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val data = db.parkingDao().getAll()
            adapter.updateData(data)
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
