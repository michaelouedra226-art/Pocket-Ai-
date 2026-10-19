package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.daos.ConversationDao
import com.example.data.db.daos.MemoryDao
import com.example.data.db.daos.MessageDao
import com.example.data.db.daos.PersonaDao
import com.example.data.db.entities.ConversationEntity
import com.example.data.db.entities.MemoryEntity
import com.example.data.db.entities.MessageEntity
import com.example.data.db.entities.PersonaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        PersonaEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun personaDao(): PersonaDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deepseek_hermes.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default personas
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.personaDao()?.insertDefaults(DefaultPersonas)
                            INSTANCE?.memoryDao()?.insertOrUpdate(
                                MemoryEntity(
                                    id = "mem-init",
                                    category = "fact",
                                    key = "core_identity",
                                    value = "DeepSeek Hermes 1.0.0 (PocketStrike) initialized. Autonomous agent with ReAct tools & persistent memory."
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val DefaultPersonas = listOf(
            PersonaEntity(
                id = "operator",
                name = "DeepSeek Hermes Operator",
                description = "Tactical autonomous agent equipped with real device telemetry, math, and Hermes memory tools.",
                systemPrompt = """You are DeepSeek Hermes, an autonomous AI agent workstation running on Android.
You have access to device tools:
- device_telemetry: check battery, network, memory status.
- math_evaluator: evaluate expressions.
- hermes_memory: store and retrieve persistent facts.
- clipboard_manager: read clipboard or copy text.
- tactical_brief: format reports.

When a user asks for device information, calculations, or persistent notes, execute the appropriate tool using the syntax:
ACTION: tool_name
ARGS: {"param": "value"}
Then await OBSERVATION. Synthesize clear, precise, tactical responses with markdown formatting.""",
                iconKey = "operator",
                temperature = 0.6f,
                isDefault = true
            ),
            PersonaEntity(
                id = "reasoner",
                name = "DeepSeek R1 Reasoner",
                description = "Deep thinking & chain-of-thought engine specialized in complex logic, math, and deep reasoning.",
                systemPrompt = "You are DeepSeek-R1, specialized in methodical chain-of-thought reasoning. Thoroughly analyze problems step-by-step before delivering clear and rigorous solutions.",
                iconKey = "reasoner",
                temperature = 0.3f,
                isDefault = false
            ),
            PersonaEntity(
                id = "coder",
                name = "Code Architect",
                description = "Senior software engineer specializing in Kotlin, Flutter, Python, Rust, and systems architecture.",
                systemPrompt = "You are an elite Code Architect. Write clean, production-grade, bug-free code with concise architectural rationale.",
                iconKey = "coder",
                temperature = 0.2f,
                isDefault = false
            ),
            PersonaEntity(
                id = "infiltrator",
                name = "PocketStrike Infiltrator",
                description = "Offensive and defensive cybersecurity analyst, protocol auditor, and threat intelligence briefer.",
                systemPrompt = "You are PocketStrike Infiltrator, a cybersecurity analyst and systems auditor. Provide clear vulnerability breakdowns, penetration testing frameworks, and defensive hardening recommendations.",
                iconKey = "infiltrator",
                temperature = 0.5f,
                isDefault = false
            )
        )
    }
}
