package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepSeekCyan
import com.example.ui.theme.StrikeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun ChatInputBar(
    isThinking: Boolean,
    toolsEnabled: Boolean,
    onToggleTools: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenPersonas: () -> Unit,
    personaName: String,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "Télémétrie de l'appareil" to "Vérifie l'état de la batterie, du réseau et de la mémoire avec device_telemetry",
        "Rapport tactique" to "Génère un brief tactique de reconnaissance",
        "Calcul" to "Calcule 256 * 1024 / 8",
        "Sauvegarder note" to "Mémorise dans hermes_memory ma clé de mission",
        "Horloge UTC" to "Quelle est l'heure système actuelle ?"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(width = 1.dp, color = DarkSurfaceBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .navigationBarsPadding()
            .padding(10.dp)
    ) {
        // Quick suggestions carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tools toggle chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (toolsEnabled) StrikeEmerald.copy(alpha = 0.15f) else DarkSurfaceElevated,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (toolsEnabled) StrikeEmerald else DarkSurfaceBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onToggleTools() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Outils",
                        tint = if (toolsEnabled) StrikeEmerald else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (toolsEnabled) "Outils: ACTIFS" else "Outils: DÉSACTIVÉS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (toolsEnabled) StrikeEmerald else TextSecondary
                    )
                }
            }

            // Quick prompts chips
            quickPrompts.forEach { (label, prompt) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { text = prompt }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = DeepSeekCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Input field and send button
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = "Donnez un ordre ou une question...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .testTag("chat_input_field"),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank() && !isThinking) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank() || isThinking,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isThinking) CrimsonAlert else DeepSeekCyan,
                    contentColor = VoidBlack
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("send_button")
            ) {
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VoidBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
