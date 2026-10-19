package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.ChatInputBar
import com.example.ui.components.DrawerContent
import com.example.ui.components.MemoryDialog
import com.example.ui.components.MessageBubble
import com.example.ui.components.PersonasDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ToolHubDialog
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepSeekCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StrikeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConv by viewModel.currentConversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val personas by viewModel.personas.collectAsStateWithLifecycle()
    val currentPersona by viewModel.currentPersona.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val isThinking by viewModel.isAgentThinking.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showPersonasDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                conversations = conversations,
                currentConversationId = currentConv?.id,
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNewConversation = {
                    viewModel.createNewConversation()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onOpenPersonas = {
                    showPersonasDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenMemory = {
                    showMemoryDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenTools = {
                    showToolsDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    showSettingsDialog = true
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DEEPSEEK HERMES",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isThinking) AmberGlow else StrikeEmerald)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${settings.provider.displayName} • ${settings.model}",
                                    fontSize = 11.sp,
                                    color = DeepSeekCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${currentPersona.name.take(16)}...]",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = DeepSeekCyan)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showPersonasDialog = true }) {
                            Icon(Icons.Default.Person, contentDescription = "Personas", tint = DeepSeekCyan)
                        }
                        IconButton(onClick = { showToolsDialog = true }) {
                            Icon(Icons.Default.Build, contentDescription = "Outils", tint = AmberGlow)
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = TextPrimary
                    ),
                    modifier = Modifier.border(width = 1.dp, color = DarkSurfaceBorder)
                )
            },
            bottomBar = {
                ChatInputBar(
                    isThinking = isThinking,
                    toolsEnabled = settings.toolsEnabled,
                    onToggleTools = {
                        viewModel.updateSettings(settings.copy(toolsEnabled = !settings.toolsEnabled))
                    },
                    onSendMessage = { prompt ->
                        viewModel.sendMessage(prompt)
                    },
                    onOpenPersonas = { showPersonasDialog = true },
                    personaName = currentPersona.name,
                    modifier = Modifier.imePadding()
                )
            },
            containerColor = DarkBackground,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    EmptyWorkspaceState(
                        onQuickPrompt = { prompt -> viewModel.sendMessage(prompt) },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(message = msg)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showPersonasDialog) {
        PersonasDialog(
            personas = personas,
            currentPersona = currentPersona,
            onSelectPersona = { viewModel.setPersona(it) },
            onSaveCustomPersona = { name, desc, prompt, temp ->
                viewModel.saveCustomPersona(name, desc, prompt, temp)
            },
            onDismiss = { showPersonasDialog = false }
        )
    }

    if (showMemoryDialog) {
        MemoryDialog(
            memories = memories,
            onSaveMemory = { k, v, cat -> viewModel.saveMemory(k, v, cat) },
            onDeleteMemory = { id -> viewModel.deleteMemory(id) },
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showToolsDialog) {
        ToolHubDialog(onDismiss = { showToolsDialog = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onClearAllData = { viewModel.clearAllConversations() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun EmptyWorkspaceState(
    onQuickPrompt: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DeepSeekCyan.copy(alpha = 0.12f))
                .border(2.dp, DeepSeekCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = DeepSeekCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DEEPSEEK HERMES",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = TextPrimary,
            letterSpacing = 1.sp
        )

        Text(
            text = "Station d'Agent IA Autonome • PocketStrike v1.0.0",
            fontSize = 12.sp,
            color = StrikeEmerald,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Text(
            text = "Exécutez des ordres, inspectez la télémétrie matérielle, effectuez des calculs ou conversez avec DeepSeek V3, R1, Gemini ou Claude.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick action cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickLaunchCard(
                title = "⚡ Télémétrie & État Système",
                description = "Interroge la batterie, le réseau, la RAM et le stockage",
                onClick = { onQuickPrompt("Exécute l'outil device_telemetry et donne-moi l'état complet du système.") }
            )
            QuickLaunchCard(
                title = "📊 Générer un Brief Tactique",
                description = "Synthèse opérationnelle de reconnaissance",
                onClick = { onQuickPrompt("Génère un brief tactique complet de l'appareil.") }
            )
            QuickLaunchCard(
                title = "🧠 Mémoire & Paramètres",
                description = "Configurer les clés API ou consulter les connaissances",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun QuickLaunchCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepSeekCyan)
            Text(description, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
