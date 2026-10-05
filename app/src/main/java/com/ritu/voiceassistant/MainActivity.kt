package com.ritu.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ritu.voiceassistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val settings by lazy { getSharedPreferences("voice_assistant", MODE_PRIVATE) }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "Mic permission diya gaya.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Mic permission chahiye.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettingsIntoUi()

        binding.startStopButton.setOnClickListener {
            saveSettingsFromUi()
            if (VoiceAssistantService.isRunning) {
                VoiceAssistantService.stop(this)
                binding.startStopButton.text = "Start assistant"
                appendLog("Assistant stop hua.")
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@setOnClickListener
                }
                VoiceAssistantService.start(this)
                binding.startStopButton.text = "Stop assistant"
                appendLog("Assistant start hua.")
            }
        }

        binding.micPermissionButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                Toast.makeText(this, "Mic permission available hai.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.overlayPermissionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission ready hai.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.batteryButton.setOnClickListener {
            val powerManager = getSystemService(PowerManager::class.java)
            if (powerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Battery optimization ignore already enabled.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.offlineDownloadButton.setOnClickListener {
            Toast.makeText(
                this,
                "Google app > Settings > Voice > Offline speech recognition > Hindi (India)",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                Log.e("MainActivity", "Settings launch failed", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.startStopButton.text = if (VoiceAssistantService.isRunning) "Stop assistant" else "Start assistant"
        binding.logTextView.text = settings.getString("log_text", "Logs:\n")
    }

    private fun loadSettingsIntoUi() {
        binding.apiKeyInput.setText(settings.getString("api_key", ""))
        binding.wakeWordsInput.setText(settings.getString("wake_words", "ritu, hey ritu"))
        binding.voiceInput.setText(settings.getString("voice", "Gemini default voice"))
        binding.languageInput.setText(settings.getString("language", "hi-IN,en-IN"))
        binding.offlineCheckbox.isChecked = settings.getBoolean("prefer_offline", true)
        binding.modelInput.setText(settings.getString("model", "gemini-3.5-flash-lite"))
        binding.voiceModelInput.setText(settings.getString("voice_model", "gemini-tts"))
        binding.systemPromptInput.setText(settings.getString("system_prompt", getString(R.string.default_system_prompt)))
    }

    private fun saveSettingsFromUi() {
        val editor = settings.edit()
        editor.putString("api_key", binding.apiKeyInput.text?.toString()?.trim())
        editor.putString("wake_words", binding.wakeWordsInput.text?.toString()?.trim())
        editor.putString("voice", binding.voiceInput.text?.toString()?.trim())
        editor.putString("language", binding.languageInput.text?.toString()?.trim())
        editor.putBoolean("prefer_offline", binding.offlineCheckbox.isChecked)
        editor.putString("model", binding.modelInput.text?.toString()?.trim())
        editor.putString("voice_model", binding.voiceModelInput.text?.toString()?.trim())
        editor.putString("system_prompt", binding.systemPromptInput.text?.toString()?.trim())
        editor.apply()
    }

    private fun appendLog(message: String) {
        val current = settings.getString("log_text", "Logs:\n") ?: "Logs:\n"
        val builder = StringBuilder(current)
        builder.append("\n")
        builder.append(message)
        binding.logTextView.text = builder.toString()
        settings.edit().putString("log_text", builder.toString()).apply()
    }
}
