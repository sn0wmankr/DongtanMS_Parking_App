package com.dongtanms.parking

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt
import kotlin.collections.forEach  // ✅ forEach import 추가

class AdminActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var tvDoneRate: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnClear: Button
    private val handler = Handler(Looper.getMainLooper())
    private val autoLockDelay = 60_000L   // 1분 후 자동 잠금
    private val autoLock = Runnable { finish() }
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        tvTotal = findViewById(R.id.tvTotal)
        tvDoneRate = findViewById(R.id.tvDoneRate)
        tvLog = findViewById(R.id.tvLog)
        btnClear = findViewById(R.id.btnClear)

        btnClear.setOnClickListener { clearAll() }
        refreshStats()

        val btnBackup = findViewById<Button>(R.id.btnBackup)
        val btnRestore = findViewById<Button>(R.id.btnRestore)
        btnBackup.setOnClickListener { backupData() }
        btnRestore.setOnClickListener { restoreData() }

        val chartHourly = findViewById<ChartView>(R.id.chartHourly)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AdminActivity)
            val list = db.parkingDao().getAll()

            val hourly = IntArray(24)
            list.forEach {
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = it.createdAt
                }
                hourly[cal.get(java.util.Calendar.HOUR_OF_DAY)]++
            }

            chartHourly.setData(hourly)
        }

        writeLog("관리자 로그인")
        scheduleAutoLock()

        ttsManager = TTSManager(this)
        ttsManager.speak("관리자 모드로 전환합니다")
    }

    private fun refreshStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AdminActivity)
            val list = db.parkingDao().getAll()
            val total = list.size
            val done = list.count { it.status == "done" }
            val rate = if (total == 0) 0 else (done * 100f / total).roundToInt()
            tvTotal.text = "총 등록 차량 : $total 대"
            tvDoneRate.text = "완료 비율 : $rate%"
        }
    }

    private fun clearAll() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AdminActivity)
            db.parkingDao().deleteAll()
            refreshStats()
            writeLog("전체 삭제 수행")
        }
    }

    private fun writeLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
            .format(System.currentTimeMillis())
        tvLog.append("[$time] $msg\n")
    }

    private fun scheduleAutoLock() {
        handler.postDelayed(autoLock, autoLockDelay)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(autoLock)
        scheduleAutoLock()
    }

    private fun backupData() {
        lifecycleScope.launch(Dispatchers.IO) {   // ✅ IO 스레드에서 실행
            val db = AppDatabase.getInstance(this@AdminActivity)
            val list = db.parkingDao().getAll()

            val jsonArray = JSONArray()
            list.forEach { entry ->
                val obj = JSONObject()
                obj.put("plateNumber", entry.plateNumber)
                obj.put("status", entry.status)
                obj.put("createdAt", entry.createdAt)
                jsonArray.put(obj)
            }

            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "parking_backup.json"
            )
            file.writeText(jsonArray.toString(2), Charsets.UTF_8)

            withContext(Dispatchers.Main) {
                writeLog("데이터 백업 완료 → ${file.absolutePath}")
            }
        }
    }

    private fun restoreData() {
        lifecycleScope.launch(Dispatchers.IO) {   // ✅ IO 스레드에서 실행
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "parking_backup.json"
            )
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    writeLog("복원 실패: 백업 파일 없음")
                }
                return@launch
            }

            val db = AppDatabase.getInstance(this@AdminActivity)
            val jsonArray = JSONArray(file.readText())

            for (i in 0 until jsonArray.length()) {   // ✅ forEach 대신 안전한 반복문
                val item = jsonArray.getJSONObject(i)
                val entry = ParkingEntry(
                    plateNumber = item.getString("plateNumber"),
                    status = item.getString("status"),
                    createdAt = item.getLong("createdAt")
                )
                db.parkingDao().insert(entry)
            }

            withContext(Dispatchers.Main) {
                writeLog("데이터 복원 완료 (${jsonArray.length()}건)")
                refreshStats()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoLock)
        super.onDestroy()
        ttsManager.shutdown()
    }
}
