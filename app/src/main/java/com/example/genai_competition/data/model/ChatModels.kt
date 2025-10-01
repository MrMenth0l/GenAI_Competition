package com.example.genai_competition.data.model

import android.net.Uri
import java.time.Instant
import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class AttachmentType {
    IMAGE,
    PDF
}

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val type: AttachmentType,
    val summary: String? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val attachments: List<ChatAttachment> = emptyList(),
    val isStreaming: Boolean = false,
    val error: String? = null
)
