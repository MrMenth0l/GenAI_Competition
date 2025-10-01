package com.example.genai_competition.data.network

import com.example.genai_competition.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject

class OpenAIService(
    private val apiKeyProvider: () -> String,
    private val baseUrl: String = "https://api.openai.com/v1/chat/completions",
    okHttpClient: OkHttpClient? = null
) {

    private val client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }
                )
            }
        }
        .build()

    suspend fun createChatCompletion(request: OpenAIChatRequest): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("OpenAI API key is not configured."))
        }

        val json = JSONObject().apply {
            put("model", request.model)
            put("temperature", request.temperature)
            put(
                "messages",
                JSONArray().apply {
                    request.messages.forEach { message ->
                        put(
                            JSONObject().apply {
                                put("role", message.role)
                                put("content", message.content)
                            }
                        )
                    }
                }
            )
        }

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrEmpty()) {
                    val message = body?.let { parseErrorMessage(it) }
                        ?: "OpenAI request failed with HTTP ${response.code}"
                    Result.failure(IOException(message))
                } else {
                    val content = extractMessageContent(body)
                    if (content != null) {
                        Result.success(content)
                    } else {
                        Result.failure(IOException("OpenAI response did not include assistant content."))
                    }
                }
            }
        } catch (io: IOException) {
            Result.failure(io)
        } catch (t: Throwable) {
            Result.failure(IOException("Unexpected error communicating with OpenAI", t))
        }
    }

    private fun extractMessageContent(body: String): String? {
        return runCatching {
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message") ?: return null
            message.optString("content").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseErrorMessage(body: String): String {
        return runCatching {
            val root = JSONObject(body)
            root.optJSONObject("error")?.optString("message")
        }.getOrNull() ?: body
    }
}
