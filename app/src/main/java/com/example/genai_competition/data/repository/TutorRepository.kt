package com.example.genai_competition.data.repository

import com.example.genai_competition.data.course.Course
import com.example.genai_competition.data.course.StudentProfile
import com.example.genai_competition.data.model.ChatMessage
import com.example.genai_competition.data.model.ChatRole
import com.example.genai_competition.data.network.OpenAIChatRequest
import com.example.genai_competition.data.network.OpenAIMessage
import com.example.genai_competition.data.network.OpenAIService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TutorRepository(
    private val openAIService: OpenAIService,
    private val studentProfile: StudentProfile,
    private val model: String = "gpt-4o-mini"
) {

    suspend fun requestTutorResponse(
        course: Course,
        history: List<ChatMessage>,
        latestUserMessage: ChatMessage
    ): Result<ChatMessage> {
        val messages = buildMessages(course, history, latestUserMessage)
        val request = OpenAIChatRequest(
            model = model,
            messages = messages
        )

        return openAIService.createChatCompletion(request).map { content ->
            ChatMessage(
                role = ChatRole.ASSISTANT,
                content = content
            )
        }
    }

    private fun buildMessages(
        course: Course,
        history: List<ChatMessage>,
        latestUserMessage: ChatMessage
    ): List<OpenAIMessage> {
        val allHistory = history + latestUserMessage
        val systemPrompt = OpenAIMessage(
            role = "system",
            content = buildSystemPrompt(course)
        )
        val conversation = allHistory.map { message ->
            OpenAIMessage(
                role = message.role.toApiRole(),
                content = renderMessageContent(message)
            )
        }
        return listOf(systemPrompt) + conversation
    }

    private fun buildSystemPrompt(course: Course): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        val today = LocalDate.now().format(formatter)
        val pensum = course.pensum
            .mapIndexed { index, topic -> "${index + 1}. $topic" }
            .joinToString(separator = "\n")

        return buildString {
            appendLine("You are a meticulous computer science study tutor.")
            appendLine("Support a student following this profile:")
            appendLine("- Name: ${studentProfile.name}")
            appendLine("- Program: ${studentProfile.program}")
            appendLine("- Academic year: ${studentProfile.currentYear}")
            appendLine()
            appendLine("The student is currently taking the course ${course.name} with ${course.professor}.")
            appendLine("Course term: ${course.term}. Today's date: $today.")
            appendLine("Class pens (syllabus focus):")
            appendLine(pensum)
            appendLine()
            appendLine("Instruction:")
            appendLine("- Ground explanations on the pensum and the student's academic level.")
            appendLine("- Ask clarifying questions when needed to understand their goals or blockers.")
            appendLine("- Provide concrete study strategies, practice suggestions, and follow-up assignments.")
            appendLine("- Reference attachments provided by the student explicitly when relevant.")
        }
    }

    private fun renderMessageContent(message: ChatMessage): String {
        if (message.attachments.isEmpty()) {
            return message.content.ifBlank { "(No text message provided)" }
        }
        val attachmentsDescription = message.attachments.joinToString(separator = "\n") { attachment ->
            "- ${attachment.type.name.lowercase().replaceFirstChar(Char::uppercase)}: ${attachment.name}"
        }

        val base = message.content.ifBlank { "(No text message provided)" }
        return buildString {
            appendLine(base)
            appendLine()
            appendLine("Attachments:")
            appendLine(attachmentsDescription)
        }
    }

    private fun ChatRole.toApiRole(): String = when (this) {
        ChatRole.USER -> "user"
        ChatRole.ASSISTANT -> "assistant"
        ChatRole.SYSTEM -> "system"
    }
}
