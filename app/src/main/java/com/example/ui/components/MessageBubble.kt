package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entities.MessageEntity
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageEntity,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"
    val isAssistant = message.role == "assistant"

    val context = LocalContext.current
    var showThinking by remember { mutableStateOf(false) }
    var showToolDetails by remember { mutableStateOf(false) }

    val timeString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("message_bubble_${message.id}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role & time header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isUser -> DeepSeekCyan
                            isTool -> AmberGlow
                            message.status == "error" -> CrimsonAlert
                            else -> StrikeEmerald
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    isUser -> "VOUS"
                    isTool -> "OBSERVATION OUTIL [${message.toolName ?: "Agent"}]"
                    else -> "DEEPSEEK HERMES"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isUser -> DeepSeekCyan
                    isTool -> AmberGlow
                    else -> StrikeEmerald
                },
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeString,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }

        // Thinking accordeon (if present)
        if (!message.thinking.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                    .clickable { showThinking = !showThinking },
                color = DarkSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Raisonnement",
                                tint = DeepSeekCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chaîne de réflexion (Thinking Chain)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepSeekCyan
                            )
                        }
                        Icon(
                            imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Basculer",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AnimatedVisibility(visible = showThinking) {
                        Text(
                            text = message.thinking,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        // Tool execution card (if message is a tool or contains tool call)
        if (!message.toolName.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { showToolDetails = !showToolDetails },
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Outil",
                                tint = AmberGlow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTION: ${message.toolName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGlow,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (showToolDetails) "Masquer JSON" else "Inspecter JSON",
                            fontSize = 10.sp,
                            color = DeepSeekCyan
                        )
                    }
                    AnimatedVisibility(visible = showToolDetails) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            if (!message.toolArgsJson.isNullOrBlank()) {
                                Text("Arguments:", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    text = message.toolArgsJson,
                                    fontSize = 11.sp,
                                    color = TextCode,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(VoidBlack, RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                )
                            }
                            if (!message.toolResultJson.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Résultat brut:", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    text = message.toolResultJson,
                                    fontSize = 11.sp,
                                    color = DeepSeekCyan,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(VoidBlack, RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main text card
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUser -> DeepSeekCyan.copy(alpha = 0.15f)
                    isTool -> AmberGlow.copy(alpha = 0.08f)
                    message.status == "error" -> CrimsonAlert.copy(alpha = 0.15f)
                    else -> DarkSurfaceElevated
                }
            ),
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = when {
                        isUser -> DeepSeekCyan.copy(alpha = 0.4f)
                        isTool -> AmberGlow.copy(alpha = 0.3f)
                        message.status == "error" -> CrimsonAlert.copy(alpha = 0.5f)
                        else -> DarkSurfaceBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Render markdown content or raw text
                FormattedMarkdownContent(text = message.content)

                // Copy button for assistant responses
                if (isAssistant || isTool) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DeepSeek Hermes Output", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMarkdownContent(text: String) {
    // Parse code blocks vs regular text
    val parts = remember(text) { splitCodeBlocks(text) }

    Column {
        parts.forEach { part ->
            if (part.isCode) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = VoidBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (part.language.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = part.language.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = part.content,
                            color = TextCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Text(
                    text = part.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private data class ContentSegment(val content: String, val isCode: Boolean, val language: String = "")

private fun splitCodeBlocks(text: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val codeFenceRegex = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```")

    var lastIndex = 0
    codeFenceRegex.findAll(text).forEach { match ->
        val range = match.range
        if (range.first > lastIndex) {
            val textBefore = text.substring(lastIndex, range.first)
            if (textBefore.isNotBlank()) {
                segments.add(ContentSegment(textBefore, isCode = false))
            }
        }
        val lang = match.groupValues.getOrNull(1).orEmpty()
        val code = match.groupValues.getOrNull(2).orEmpty()
        segments.add(ContentSegment(code, isCode = true, language = lang))
        lastIndex = range.last + 1
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        if (remaining.isNotBlank()) {
            segments.add(ContentSegment(remaining, isCode = false))
        }
    }

    if (segments.isEmpty()) {
        segments.add(ContentSegment(text, isCode = false))
    }

    return segments
}
