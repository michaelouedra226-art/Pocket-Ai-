package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.AppDatabase
import com.example.data.tools.DeviceToolService
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepSeekCyan
import com.example.ui.theme.StrikeEmerald
import com.example.ui.theme.TextCode
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ToolHubDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toolService = remember {
        val db = AppDatabase.getInstance(context)
        DeviceToolService(context, db.memoryDao())
    }

    var telemetryJson by remember { mutableStateOf<JSONObject?>(null) }
    var testResultOutput by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val res = toolService.executeTool("device_telemetry", JSONObject())
        telemetryJson = try { JSONObject(res.rawJson) } catch (_: Exception) { null }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkBackground,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AmberGlow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HUB OUTILS & TÉLÉMÉTRIE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Outils matériels et analytiques locaux que l'agent DeepSeek Hermes peut invoquer de manière autonome.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Telemetry cards grid
                val batt = telemetryJson?.optJSONObject("battery")
                val net = telemetryJson?.optJSONObject("network")
                val mem = telemetryJson?.optJSONObject("memory")
                val hw = telemetryJson?.optJSONObject("hardware")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "BATTERIE",
                        value = "${batt?.optInt("level") ?: "--"}%",
                        subtitle = batt?.optString("status") ?: "Inconnu",
                        icon = Icons.Default.BatteryChargingFull,
                        accent = StrikeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "RÉSEAU",
                        value = net?.optString("type") ?: "Vérification...",
                        subtitle = if (net?.optBoolean("has_internet") == true) "En ligne" else "Hors ligne",
                        icon = Icons.Default.Wifi,
                        accent = DeepSeekCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "MÉMOIRE RAM",
                        value = "${mem?.optInt("avail_ram_mb") ?: "--"} MB",
                        subtitle = "Dispo / ${mem?.optInt("total_ram_mb") ?: "--"} MB",
                        icon = Icons.Default.Memory,
                        accent = AmberGlow,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "TERMINAL",
                        value = hw?.optString("model") ?: "Android",
                        subtitle = "Android ${hw?.optString("android_version") ?: ""}",
                        icon = Icons.Default.PhoneAndroid,
                        accent = Color(0xFFB388FF),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TEST RAPIDE DES OUTILS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val res = toolService.executeTool("device_telemetry", JSONObject())
                                testResultOutput = res.output
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Télémétrie", fontSize = 11.sp, color = DeepSeekCyan)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val res = toolService.executeTool("math_evaluator", JSONObject().apply { put("expression", "sqrt(144) * 8 + 32") })
                                testResultOutput = res.output
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calcul", fontSize = 11.sp, color = AmberGlow)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val res = toolService.executeTool("timestamp_clock", JSONObject())
                                testResultOutput = res.output
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Horloge", fontSize = 11.sp, color = StrikeEmerald)
                    }
                }

                if (!testResultOutput.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sortie d'exécution de l'outil:", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = testResultOutput.orEmpty(),
                        fontSize = 11.sp,
                        color = TextCode,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VoidBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        modifier = modifier.border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = accent)
        }
    }
}
