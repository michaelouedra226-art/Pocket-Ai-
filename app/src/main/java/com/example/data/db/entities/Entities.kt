package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val model: String = "deepseek-chat",
    val provider: String = "DeepSeek",
    val personaId: String = "operator",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val thinking: String? = null,
    val toolName: String? = null,
    val toolArgsJson: String? = null,
    val toolResultJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "complete" // "streaming", "complete", "error"
)

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val iconKey: String = "operator",
    val temperature: Float = 0.7f,
    val isDefault: Boolean = false
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val category: String, // "fact", "preference", "credential", "task"
    val key: String,
    val value: String,
    val confidence: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis()
)
