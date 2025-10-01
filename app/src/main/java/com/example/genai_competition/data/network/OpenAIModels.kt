package com.example.genai_competition.data.network

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.2
)
