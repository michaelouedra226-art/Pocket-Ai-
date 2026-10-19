package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.ProviderType
import com.example.ui.SettingsState
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepSeekCyan
import com.example.ui.theme.StrikeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun SettingsDialog(
    settings: SettingsState,
    onSaveSettings: (SettingsState) -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(settings.provider) }
    var modelName by remember { mutableStateOf(settings.model) }
    var deepseekKey by remember { mutableStateOf(settings.deepseekApiKey) }
    var geminiKey by remember { mutableStateOf(settings.geminiApiKey) }
    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
    var claudeKey by remember { mutableStateOf(settings.claudeApiKey) }
    var customBaseUrl by remember { mutableStateOf(settings.customBaseUrl) }
    var temperature by remember { mutableFloatStateOf(settings.temperature) }
    var maxTokens by remember { mutableIntStateOf(settings.maxTokens) }
    var toolsEnabled by remember { mutableStateOf(settings.toolsEnabled) }

    var showKey by remember { mutableStateOf(false) }

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
                                .background(DeepSeekCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = DeepSeekCyan, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PARAMÈTRES AGENT & API",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("FOURNISSEUR ACTIF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // Provider selection chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProviderType.values().forEach { provider ->
                        val isSelected = provider == selectedProvider
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DeepSeekCyan.copy(alpha = 0.15f) else DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) DeepSeekCyan else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedProvider = provider
                                    modelName = provider.defaultModel
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = provider.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DeepSeekCyan else TextPrimary
                                )
                                Text(
                                    text = provider.defaultModel,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model name field
                TextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Nom du modèle", color = TextSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active API key input
                val (currentKeyVal, onKeyChange, labelText) = when (selectedProvider) {
                    ProviderType.DEEPSEEK -> Triple(deepseekKey, { k: String -> deepseekKey = k }, "Clé API DeepSeek (sk-...)")
                    ProviderType.GEMINI -> Triple(geminiKey, { k: String -> geminiKey = k }, "Clé API Google Gemini")
                    ProviderType.OPENAI -> Triple(openaiKey, { k: String -> openaiKey = k }, "Clé API OpenAI")
                    ProviderType.CLAUDE -> Triple(claudeKey, { k: String -> claudeKey = k }, "Clé API Anthropic Claude")
                    ProviderType.LOCAL_GGUF -> Triple(customBaseUrl, { k: String -> customBaseUrl = k }, "URL endpoint local (ex: http://10.0.2.2:11434/v1)")
                }

                TextField(
                    value = currentKeyVal,
                    onValueChange = onKeyChange,
                    label = { Text(labelText, color = TextSecondary) },
                    visualTransformation = if (showKey || selectedProvider == ProviderType.LOCAL_GGUF) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (selectedProvider != ProviderType.LOCAL_GGUF) {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ReAct tools toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Exécution autonome des outils ReAct", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Permet à l'agent de lire la batterie, le réseau, et d'exécuter des calculs", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = toolsEnabled,
                        onCheckedChange = { toolsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoidBlack,
                            checkedTrackColor = StrikeEmerald
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Temperature slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Température (Créativité)", fontSize = 12.sp, color = TextSecondary)
                    Text("${(temperature * 100).toInt() / 100f}", fontSize = 12.sp, color = DeepSeekCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..1.2f,
                    colors = SliderDefaults.colors(thumbColor = DeepSeekCyan, activeTrackColor = DeepSeekCyan)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Max Tokens slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tokens de sortie maximum (Max Tokens)", fontSize = 12.sp, color = TextSecondary)
                    Text("$maxTokens", fontSize = 12.sp, color = DeepSeekCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.toInt() },
                    valueRange = 1024f..8192f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = DeepSeekCyan, activeTrackColor = DeepSeekCyan)
                )

                HorizontalDivider(color = DarkSurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Clear history button
                Button(
                    onClick = {
                        onClearAllData()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RÉINITIALISER TOUTES LES MISSIONS", color = CrimsonAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save button
                Button(
                    onClick = {
                        onSaveSettings(
                            SettingsState(
                                provider = selectedProvider,
                                model = modelName,
                                deepseekApiKey = deepseekKey,
                                geminiApiKey = geminiKey,
                                openaiApiKey = openaiKey,
                                claudeApiKey = claudeKey,
                                customBaseUrl = customBaseUrl,
                                temperature = temperature,
                                maxTokens = maxTokens,
                                toolsEnabled = toolsEnabled
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSeekCyan, contentColor = VoidBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ENREGISTRER LES PARAMÈTRES", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
