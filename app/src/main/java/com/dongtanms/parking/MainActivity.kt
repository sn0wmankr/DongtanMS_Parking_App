package com.dongtanms.parking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        // RecyclerView Adapter 초기화
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

        // ✅ TTS Manager 초기화 (음성 안내 담당)
        ttsManager = TTSManager(this)

        // 5분마다 자동 백업 예약
        handler.postDelayed(autoBackupRunnable, 5 * 60 * 1000)

        // 숫자패드 초기화
        setupNumberPad()

        // 파일 접근 권한 요청 (Android 10 이하)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 100
            )
        }
    }

    // ===============================
    // 숫자패드 관련
    // ===============================
    private fun setupNumberPad() {
        val btnIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        // 숫자 버튼
        btnIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                if (input.length < 4) {
                    input.append((it as Button).text)
                    updateInput()
                }
            }
        }

        // ← 버튼
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (input.isNotEmpty()) {
                input.deleteCharAt(input.length - 1)
                updateInput()
            }
        }

        // 등록 버튼
        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val number = input.toString()
            when (number) {
                "8007" -> { // ✅ 관리자 진입
                    startActivity(Intent(this, AdminActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    input.clear()
                    ttsManager.speak("관리자 모드로 전환합니다")
                }
                else -> {
                    if (number.length == 4) {
                        registerCar(number)
                        input.clear()
                    }
                }
            }
            updateInput()
        }
    }

    private fun updateInput() {
        inputView.text = input.toString().padEnd(4, '_')
    }

    // ===============================
    // 차량 등록 / 삭제 / 완료 처리
    // ===============================
    private fun registerCar(number: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().insert(ParkingEntry(plateNumber = number))
            ttsManager.speak("$number 번 차량 등록되었습니다")
            loadParkingList()
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun deleteCar(entry: ParkingEntry) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().delete(entry)
            loadParkingList()
            ttsManager.speak("${entry.plateNumber} 번 차량이 삭제되었습니다")
            BackupManager.autoBackup(this@MainActivity)
        }
    }

    private fun markAsDone(entry: ParkingEntry) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            db.parkingDao().update(entry.copy(status = "done"))
            loadParkingList()
            ttsManager.speak("${entry.plateNumber} 번 차량 완료 처리되었습니다")
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

    // ===============================
    // 종료 시
    // ===============================
    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        handler.removeCallbacks(autoBackupRunnable)
        lifecycleScope.launch {
            BackupManager.autoBackup(this@MainActivity)
        }
    }
}
