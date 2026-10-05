package com.ritu.voiceassistant

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient(
    private val apiKey: String,
    private val model: String = "gemini-3.5-flash-lite"
) {
    fun generateText(
        prompt: String,
        systemPrompt: String,
        callback: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put("generationConfig", JSONObject().put("maxOutputTokens", 300))
        }

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        Thread {
            try {
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Gemini API error"
                    onError(errorText)
                    return@Thread
                }

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val answer = parseAnswer(responseText)
                callback(answer)
            } catch (e: Exception) {
                Log.e("GeminiClient", "Gemini call failed", e)
                onError("Gemini call failed: ${e.message}")
            }
        }.start()
    }

    private fun parseAnswer(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).optString("text", "")
            if (text.isNotBlank()) text else "Mujhe samajh nahi aaya, aap dobara boliye."
        } catch (e: Exception) {
            "Mujhe samajh nahi aaya, aap dobara boliye."
        }
    }
}
