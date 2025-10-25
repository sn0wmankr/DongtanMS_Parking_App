package com.dongtanms.parking

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingMessages = Collections.synchronizedList(mutableListOf<String>()) // 동기화된 리스트 사용
    private val ttsEngine = "com.google.android.tts" // Google TTS 엔진

    init {
        try {
            // Google TTS 엔진을 우선적으로 사용
            tts = TextToSpeech(context.applicationContext, this, ttsEngine)
            Log.d("TTSManager", "Attempting to initialize with Google TTS engine.")
        } catch (e: Exception) {
            Log.e("TTSManager", "Failed to initialize Google TTS engine, falling back to default.", e)
            // 실패 시 기본 엔진으로 재시도
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engineInfo = tts?.defaultEngine ?: "Unknown"
            Log.d("TTSManager", "TTS Initialized with engine: $engineInfo")

            val result = tts?.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Korean language is not supported or missing data on engine: $engineInfo")
                tts?.language = Locale.US
            } else {
                Log.d("TTSManager", "TTS language set to Korean.")
            }
            isInitialized = true
            processPendingMessages()
        } else {
            Log.e("TTSManager", "TTS Initialization failed with status: $status")
        }
    }

    private fun processPendingMessages() {
        synchronized(pendingMessages) {
            val iterator = pendingMessages.iterator()
            while (iterator.hasNext()) {
                val message = iterator.next()
                speakInternal(message)
                iterator.remove()
            }
        }
    }

    private fun speakInternal(message: String) {
        val result = tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
        if (result == TextToSpeech.ERROR) {
            Log.e("TTSManager", "Failed to speak message: $message")
        }
    }

    fun speak(message: String) {
        if (isInitialized) {
            val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            if (result == TextToSpeech.ERROR) {
                Log.e("TTSManager", "Failed to speak message: $message")
            }
        } else {
            Log.w("TTSManager", "TTS not initialized yet. Queuing message: $message")
            synchronized(pendingMessages) {
                pendingMessages.add(message)
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            isInitialized = false
            Log.d("TTSManager", "TTS shutdown successfully.")
        } catch (e: Exception) {
            Log.e("TTSManager", "Failed to shutdown TextToSpeech", e)
        }
    }
}
