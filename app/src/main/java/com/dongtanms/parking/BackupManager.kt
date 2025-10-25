package com.dongtanms.parking

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    suspend fun autoBackup(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val list = db.parkingDao().getAll()
            val jsonArray = JSONArray()

            list.forEach { entry ->
                val obj = JSONObject()
                obj.put("plateNumber", entry.plateNumber)
                obj.put("status", entry.status)
                obj.put("createdAt", entry.createdAt)
                jsonArray.put(obj)
            }

            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "parking_autosave.json")
            file.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
