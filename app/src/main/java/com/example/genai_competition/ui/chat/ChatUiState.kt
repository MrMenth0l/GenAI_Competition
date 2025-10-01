package com.example.genai_competition.ui.chat

import com.example.genai_competition.data.course.Course
import com.example.genai_competition.data.model.ChatMessage

data class CourseChatState(
    val course: Course,
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    val lastMessagePreview: String?
        get() = messages.lastOrNull()?.content?.takeIf { it.isNotBlank() }
}

data class TutorUiState(
    val courses: List<CourseChatState>,
    val selectedCourseId: String?
) {
    val selectedCourse: CourseChatState?
        get() = courses.firstOrNull { it.course.id == selectedCourseId }
}
