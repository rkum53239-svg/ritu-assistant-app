package com.ritu.voiceassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    fun speak(text: String, onDone: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                    speakInternal(text, onDone, onError)
                } else {
                    onError?.invoke("TTS setup failed")
                }
            }
            return
        }

        if (ready) {
            speakInternal(text, onDone, onError)
        }
    }

    private fun speakInternal(
        text: String,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            tts?.language = Locale("hi", "IN")
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ritu_tts")
            onDone?.invoke()
        } catch (e: Exception) {
            Log.e("TtsManager", "TTS failed", e)
            onError?.invoke("TTS failed: ${e.message}")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
