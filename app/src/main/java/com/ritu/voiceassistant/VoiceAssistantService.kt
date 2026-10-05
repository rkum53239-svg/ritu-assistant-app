package com.ritu.voiceassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceAssistantService : Service() {

    private lateinit var prefs: SharedPreferences
    private var recognizer: SpeechRecognizer? = null
    private var tts: TtsManager? = null
    private var lastResponseAt = 0L
    private val maxFollowupMs = 50_000L

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("voice_assistant", MODE_PRIVATE)
        tts = TtsManager(this)
        startForeground(1001, buildNotification())
        initializeRecognizer()
        startWakeListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "ritu_voice_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ritu Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ritu Voice Assistant")
            .setContentText("Listening for wake words")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun initializeRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                Log.e("VoiceAssistantService", "Speech error: $error")
                if (error == SpeechRecognizer.ERROR_CLIENT || error == 12 || error == 13) {
                    appendLog("Offline speech unavailable; switching to online recognition.")
                    startWakeListening(preferOffline = false)
                    return
                }
                startWakeListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(" ") ?: ""
                processRecognizedText(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(" ") ?: ""
                if (text.isNotEmpty()) {
                    appendLog("Partial: $text")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    private fun startWakeListening(preferOffline: Boolean = prefs.getBoolean("prefer_offline", true)) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }
        recognizer?.startListening(intent)
    }

    private fun processRecognizedText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return

        val lower = text.lowercase(Locale.getDefault())
        val wakeWords = prefs.getString("wake_words", "ritu, hey ritu")?.split(',')
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("ritu", "hey ritu")

        val followUpAllowed = System.currentTimeMillis() - lastResponseAt < maxFollowupMs
        val isWakeWord = wakeWords.any { lower.contains(it) }

        if (isWakeWord) {
            appendLog("Wake word triggered. Heard: $text")
            tts?.speak("Main sun raha hoon.")
            startCommandListening()
            return
        }

        if (lower.startsWith("jarvis")) {
            val query = lower.removePrefix("jarvis").trim()
            appendLog("Jarvis command: $text")
            if (query.isBlank()) {
                tts?.speak("Haan, boliye.")
                startCommandListening()
            } else {
                askGemini(query)
            }
            return
        }

        if (followUpAllowed && !lower.contains("ritu")) {
            appendLog("Follow-up: $text")
            askGemini(text)
            return
        }

        startWakeListening()
    }

    private fun startCommandListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, prefs.getBoolean("prefer_offline", true))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }
        recognizer?.startListening(intent)
    }

    private fun askGemini(prompt: String) {
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        val systemPrompt = prefs.getString(
            "system_prompt",
            "Aap ek Hinglish assistant ho. 1-3 chhote sentences. No markdown. No emoji."
        ) ?: "Aap ek Hinglish assistant ho. 1-3 chhote sentences. No markdown. No emoji."

        if (apiKey.isBlank()) {
            appendLog("API key missing.")
            tts?.speak("API key add karo first.")
            return
        }

        val client = GeminiClient(apiKey, model)
        client.generateText(prompt, systemPrompt, { answer ->
            lastResponseAt = System.currentTimeMillis()
            appendLog("Gemini answer: $answer")
            tts?.speak(answer)
            startWakeListening()
        }, { error ->
            appendLog("Gemini error: $error")
            tts?.speak("Gemini fail hua, phir try karo.")
            startWakeListening()
        })
    }

    private fun appendLog(message: String) {
        val text = prefs.getString("log_text", "Logs:\n") ?: "Logs:\n"
        val updated = "$text\n$message"
        prefs.edit().putString("log_text", updated).apply()
        Log.d("VoiceAssistantService", message)
    }

    companion object {
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java)
            context.startForegroundService(intent)
            isRunning = true
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java)
            context.stopService(intent)
            isRunning = false
        }
    }
}
