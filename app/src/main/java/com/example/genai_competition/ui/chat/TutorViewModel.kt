package com.example.genai_competition.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genai_competition.BuildConfig
import com.example.genai_competition.data.course.CourseCatalog
import com.example.genai_competition.data.model.ChatAttachment
import com.example.genai_competition.data.model.ChatMessage
import com.example.genai_competition.data.model.ChatRole
import com.example.genai_competition.data.network.OpenAIService
import com.example.genai_competition.data.repository.TutorRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TutorViewModel(
    private val tutorRepository: TutorRepository = TutorRepository(
        openAIService = OpenAIService(apiKeyProvider = { BuildConfig.OPENAI_API_KEY }),
        studentProfile = CourseCatalog.sampleStudent
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TutorUiState(
            courses = CourseCatalog.courses.map { CourseChatState(course = it) },
            selectedCourseId = CourseCatalog.courses.firstOrNull()?.id
        )
    )
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    fun selectCourse(courseId: String) {
        _uiState.update { state ->
            if (state.selectedCourseId == courseId) state else state.copy(selectedCourseId = courseId)
        }
    }

    fun sendMessage(courseId: String, message: String, attachments: List<ChatAttachment>) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty() && attachments.isEmpty()) {
            return
        }

        val currentState = _uiState.value
        val courseState = currentState.courses.firstOrNull { it.course.id == courseId } ?: return
        if (courseState.isSending) return

        val userMessage = ChatMessage(
            role = ChatRole.USER,
            content = trimmedMessage,
            timestamp = Instant.now(),
            attachments = attachments
        )

        val pendingState = courseState.copy(
            messages = courseState.messages + userMessage,
            isSending = true,
            errorMessage = null
        )
        updateCourseState(pendingState)

        viewModelScope.launch {
            val result = tutorRepository.requestTutorResponse(
                course = courseState.course,
                history = courseState.messages,
                latestUserMessage = userMessage
            )

            result.fold(
                onSuccess = { assistantReply ->
                    val updated = pendingState.copy(
                        messages = pendingState.messages + assistantReply,
                        isSending = false,
                        errorMessage = null
                    )
                    updateCourseState(updated)
                },
                onFailure = { throwable ->
                    val failedState = pendingState.copy(
                        isSending = false,
                        errorMessage = throwable.message ?: "Unable to reach the tutor right now."
                    )
                    updateCourseState(failedState)
                }
            )
        }
    }

    fun clearError(courseId: String) {
        val courseState = _uiState.value.courses.firstOrNull { it.course.id == courseId } ?: return
        if (courseState.errorMessage != null) {
            updateCourseState(courseState.copy(errorMessage = null))
        }
    }

    private fun updateCourseState(updatedCourse: CourseChatState) {
        _uiState.update { state ->
            val newCourses = state.courses.map { existing ->
                if (existing.course.id == updatedCourse.course.id) updatedCourse else existing
            }
            state.copy(courses = newCourses)
        }
    }
}
