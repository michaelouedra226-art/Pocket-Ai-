package com.example.data.tools

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.db.daos.MemoryDao
import com.example.data.db.entities.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val rawJson: String
)

class DeviceToolService(
    private val context: Context,
    private val memoryDao: MemoryDao
) {

    suspend fun executeTool(toolName: String, args: JSONObject): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (toolName.lowercase().trim()) {
                "device_telemetry", "get_device_status" -> {
                    val telemetry = getDeviceTelemetry()
                    ToolExecutionResult(
                        toolName = "device_telemetry",
                        success = true,
                        output = telemetry.toString(2),
                        rawJson = telemetry.toString()
                    )
                }
                "math_evaluator", "calculate" -> {
                    val expression = args.optString("expression", "")
                    val result = evaluateMathExpression(expression)
                    val json = JSONObject().apply {
                        put("expression", expression)
                        put("result", result)
                    }
                    ToolExecutionResult(
                        toolName = "math_evaluator",
                        success = true,
                        output = "Result: $result",
                        rawJson = json.toString()
                    )
                }
                "clipboard_manager" -> {
                    val action = args.optString("action", "read")
                    val textToCopy = args.optString("text", "")
                    val res = handleClipboard(action, textToCopy)
                    ToolExecutionResult(
                        toolName = "clipboard_manager",
                        success = true,
                        output = res,
                        rawJson = JSONObject().apply {
                            put("action", action)
                            put("result", res)
                        }.toString()
                    )
                }
                "timestamp_clock", "get_time" -> {
                    val now = System.currentTimeMillis()
                    val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                        timeZone = TimeZone.getDefault()
                    }
                    val json = JSONObject().apply {
                        put("timestamp_epoch_ms", now)
                        put("iso8601", sdfIso.format(Date(now)))
                        put("timezone", TimeZone.getDefault().id)
                        put("device_uptime_ms", android.os.SystemClock.uptimeMillis())
                    }
                    ToolExecutionResult(
                        toolName = "timestamp_clock",
                        success = true,
                        output = json.toString(2),
                        rawJson = json.toString()
                    )
                }
                "hermes_memory" -> {
                    val action = args.optString("action", "retrieve")
                    val key = args.optString("key", "")
                    val value = args.optString("value", "")
                    val category = args.optString("category", "fact")

                    if (action == "store" && key.isNotBlank()) {
                        val entity = MemoryEntity(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            key = key,
                            value = value
                        )
                        memoryDao.insertOrUpdate(entity)
                        val json = JSONObject().apply {
                            put("status", "saved")
                            put("key", key)
                            put("value", value)
                        }
                        ToolExecutionResult(
                            toolName = "hermes_memory",
                            success = true,
                            output = "Memory stored: [$key] -> $value",
                            rawJson = json.toString()
                        )
                    } else {
                        val results = memoryDao.searchMemories(key)
                        val json = JSONObject().apply {
                            put("query", key)
                            put("matches_count", results.size)
                            val arr = org.json.JSONArray()
                            results.forEach {
                                arr.put(JSONObject().apply {
                                    put("key", it.key)
                                    put("value", it.value)
                                    put("category", it.category)
                                })
                            }
                            put("records", arr)
                        }
                        ToolExecutionResult(
                            toolName = "hermes_memory",
                            success = true,
                            output = if (results.isEmpty()) "No memories found for '$key'" else json.toString(2),
                            rawJson = json.toString()
                        )
                    }
                }
                "tactical_brief" -> {
                    val topic = args.optString("topic", "System Status")
                    val telemetry = getDeviceTelemetry()
                    val brief = buildString {
                        appendLine("### ⚡ TACTICAL RECONNAISSANCE BRIEF")
                        appendLine("**TOPIC**: $topic")
                        appendLine("**TIMESTAMP**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                        appendLine("**TELEMETRY SUMMARY**:")
                        appendLine("- Battery: ${telemetry.optJSONObject("battery")?.optInt("level")}% (${telemetry.optJSONObject("battery")?.optString("status")})")
                        appendLine("- Network: ${telemetry.optJSONObject("network")?.optString("type")}")
                        appendLine("- Memory: ${telemetry.optJSONObject("memory")?.optString("avail_ram_mb")} MB free / ${telemetry.optJSONObject("memory")?.optString("total_ram_mb")} MB total")
                        appendLine("- Unit: ${telemetry.optJSONObject("hardware")?.optString("model")}")
                        appendLine("STATUS: OPERATIONAL [READY]")
                    }
                    ToolExecutionResult(
                        toolName = "tactical_brief",
                        success = true,
                        output = brief,
                        rawJson = JSONObject().apply { put("brief", brief) }.toString()
                    )
                }
                else -> {
                    ToolExecutionResult(
                        toolName = toolName,
                        success = false,
                        output = "Unknown tool: '$toolName'. Supported: device_telemetry, math_evaluator, clipboard_manager, timestamp_clock, hermes_memory, tactical_brief",
                        rawJson = JSONObject().apply { put("error", "Unknown tool $toolName") }.toString()
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = toolName,
                success = false,
                output = "Tool execution error: ${e.message}",
                rawJson = JSONObject().apply { put("error", e.message ?: "Unknown error") }.toString()
            )
        }
    }

    private fun getDeviceTelemetry(): JSONObject {
        val root = JSONObject()

        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL

        val batteryJson = JSONObject().apply {
            put("level", batteryPct)
            put("charging", isCharging)
            put("status", when (statusInt) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            })
        }
        root.put("battery", batteryJson)

        // Connectivity
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connMgr?.activeNetwork
        val caps = connMgr?.getNetworkCapabilities(activeNetwork)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Offline / Disconnected"
        }
        val netJson = JSONObject().apply {
            put("type", netType)
            put("has_internet", caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        }
        root.put("network", netJson)

        // Memory (RAM)
        val actMgr = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMgr?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val memJson = JSONObject().apply {
            put("total_ram_mb", totalRamMb)
            put("avail_ram_mb", availRamMb)
            put("low_memory_flag", memInfo.lowMemory)
        }
        root.put("memory", memJson)

        // Storage
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            val storageJson = JSONObject().apply {
                put("free_storage_gb", String.format(Locale.US, "%.2f", bytesAvailable / (1024.0 * 1024.0 * 1024.0)))
                put("total_storage_gb", String.format(Locale.US, "%.2f", bytesTotal / (1024.0 * 1024.0 * 1024.0)))
            }
            root.put("storage", storageJson)
        } catch (_: Exception) {}

        // Hardware
        val hwJson = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("android_version", Build.VERSION.RELEASE)
            put("api_level", Build.VERSION.SDK_INT)
            put("device", Build.DEVICE)
        }
        root.put("hardware", hwJson)

        return root
    }

    private fun handleClipboard(action: String, textToCopy: String): String {
        val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return "Clipboard service unavailable"

        return if (action.equals("copy", ignoreCase = true)) {
            val clip = ClipData.newPlainText("DeepSeek Hermes", textToCopy)
            clipMgr.setPrimaryClip(clip)
            "Copied text to clipboard."
        } else {
            val primary = clipMgr.primaryClip
            if (primary != null && primary.itemCount > 0) {
                primary.getItemAt(0).text?.toString() ?: "(empty clipboard)"
            } else {
                "(empty clipboard)"
            }
        }
    }

    private fun evaluateMathExpression(expr: String): Double {
        val clean = expr.replace(" ", "")
        // Handle common simple functions or binary ops
        return when {
            clean.startsWith("sqrt(") && clean.endsWith(")") -> {
                val sub = clean.removeSurrounding("sqrt(", ")").toDoubleOrNull() ?: 0.0
                kotlin.math.sqrt(sub)
            }
            clean.startsWith("sin(") && clean.endsWith(")") -> {
                val sub = clean.removeSurrounding("sin(", ")").toDoubleOrNull() ?: 0.0
                kotlin.math.sin(Math.toRadians(sub))
            }
            clean.startsWith("cos(") && clean.endsWith(")") -> {
                val sub = clean.removeSurrounding("cos(", ")").toDoubleOrNull() ?: 0.0
                kotlin.math.cos(Math.toRadians(sub))
            }
            clean.contains("+") -> {
                val parts = clean.split("+")
                parts.sumOf { it.toDoubleOrNull() ?: 0.0 }
            }
            clean.contains("-") && !clean.startsWith("-") -> {
                val parts = clean.split("-")
                val first = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val rest = parts.drop(1).sumOf { it.toDoubleOrNull() ?: 0.0 }
                first - rest
            }
            clean.contains("*") -> {
                val parts = clean.split("*")
                parts.fold(1.0) { acc, s -> acc * (s.toDoubleOrNull() ?: 1.0) }
            }
            clean.contains("/") -> {
                val parts = clean.split("/")
                val num = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val den = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                if (den == 0.0) Double.NaN else num / den
            }
            clean.contains("^") -> {
                val parts = clean.split("^")
                val base = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val exp = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                Math.pow(base, exp)
            }
            else -> clean.toDoubleOrNull() ?: 0.0
        }
    }
}
