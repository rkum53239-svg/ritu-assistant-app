package com.ritu.voiceassistant

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_assistant", Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString("api_key", "") ?: ""
    fun setApiKey(value: String) = prefs.edit { putString("api_key", value) }

    fun getWakeWords(): List<String> {
        val raw = prefs.getString("wake_words", "ritu, hey ritu") ?: "ritu, hey ritu"
        return raw.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    fun getPreferredOffline(): Boolean = prefs.getBoolean("prefer_offline", true)
    fun getModel(): String = prefs.getString("model", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
    fun getLanguage(): String = prefs.getString("language", "hi-IN,en-IN") ?: "hi-IN,en-IN"
    fun getSystemPrompt(): String = prefs.getString("system_prompt", "Aap ek Hinglish assistant ho.") ?: "Aap ek Hinglish assistant ho."
    fun getVoiceModel(): String = prefs.getString("voice_model", "gemini-tts") ?: "gemini-tts"

    fun log(message: String) {
        val current = prefs.getString("log_text", "Logs:\n") ?: "Logs:\n"
        val output = "$current\n$message"
        prefs.edit { putString("log_text", output) }
    }
}
