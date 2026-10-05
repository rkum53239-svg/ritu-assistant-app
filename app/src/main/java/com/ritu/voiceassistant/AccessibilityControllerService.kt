package com.ritu.voiceassistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityControllerService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityControllerService", "Connected")
    }

    fun executeTool(toolName: String, args: org.json.JSONObject?): String {
        return when (toolName.lowercase()) {
            "open_app" -> {
                val app = args?.optString("app", "") ?: ""
                openApp(app)
                "Opened app: $app"
            }
            "open_url" -> {
                val url = args?.optString("url", "") ?: ""
                openUrl(url)
                "Opened URL: $url"
            }
            "read_screen" -> readScreen()
            "tap_text" -> {
                val text = args?.optString("text", "") ?: ""
                tapText(text)
                "Tapped text: $text"
            }
            "type_text" -> {
                val text = args?.optString("text", "") ?: ""
                val value = args?.optString("value", "") ?: ""
                typeText(text, value)
                "Typed into field: $text"
            }
            "scroll" -> {
                val direction = args?.optString("direction", "down") ?: "down"
                scroll(direction)
                "Scrolled: $direction"
            }
            "go_back" -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                "Back pressed"
            }
            "go_home" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
                "Home pressed"
            }
            "wait" -> {
                val ms = args?.optLong("ms", 1000L) ?: 1000L
                Thread.sleep(ms)
                "Waited for $ms ms"
            }
            else -> "Unsupported tool: $toolName"
        }
    }

    private fun openApp(appName: String): Boolean {
        val pkg = when {
            appName.contains("whatsapp", true) -> "com.whatsapp"
            appName.contains("youtube", true) -> "com.google.android.youtube"
            appName.contains("chrome", true) -> "com.android.chrome"
            else -> appName
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            return true
        }
        return false
    }

    private fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No screen available"
        val builder = StringBuilder()
        collectText(root, builder)
        return builder.toString().trim()
    }

    private fun collectText(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return
        val text = node.text
        if (!text.isNullOrBlank()) {
            builder.append(text.toString()).append(" ")
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), builder)
        }
    }

    private fun tapText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findNodeByText(root, text)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, targetText: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString() ?: ""
        if (text.equals(targetText, true) || text.contains(targetText, true)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            if (findNodeByText(node.getChild(i), targetText)) return true
        }
        return false
    }

    private fun typeText(fieldText: String, value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, fieldText)
        if (node) {
            val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (target != null) {
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
                return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
        return false
    }

    private fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (direction.equals("up", true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return root.performAction(action)
    }

    companion object {
        fun openSystemSettings(context: android.content.Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
