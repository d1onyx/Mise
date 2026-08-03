package com.dishlab.infrastructure.ai

import com.dishlab.application.service.TagCategorizationProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class OpenRouterTagCategorizationProvider(
    private val apiKey: String,
    private val model: String = "google/gemma-4-31b-it:free",
) : TagCategorizationProvider {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = ConcurrentHashMap<String, String>()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun categorize(tag: String, availableCategories: List<String>): String {
        val key = tag.trim().lowercase()
        cache[key]?.let { return it }
        return runCatching {
            val categoriesText = availableCategories.joinToString(", ")
            val httpResponse = client.post(OPENROUTER_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                header("HTTP-Referer", "https://dishlab.app")
                header("X-Title", "DishLab Tag Categorizer")
                setBody(
                    ChatRequest(
                        model = model,
                        messages = listOf(
                            ChatMessage("system", buildPrompt(categoriesText)),
                            ChatMessage("user", tag.trim()),
                        ),
                    ),
                )
            }
            if (httpResponse.status != HttpStatusCode.OK) return "other"
            val response: ChatResponse = httpResponse.body()
            val content = response.choices.firstOrNull()?.message?.content.orEmpty().trim()
            val result = runCatching { json.decodeFromString<CategoryResult>(content) }.getOrNull()
            val category = result?.category?.trim()?.lowercase()
                ?.takeIf { it in availableCategories }
                ?: "other"
            cache[key] = category
            category
        }.getOrDefault("other")
    }

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float = 0.0f,
        @SerialName("max_tokens") val maxTokens: Int = 50,
        @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    )

    @Serializable
    private data class ResponseFormat(val type: String = "json_object")

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class CategoryResult(val category: String = "other")

    private companion object {
        const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

        fun buildPrompt(categories: String) = """
            You are a food ingredient categorizer.
            Given an ingredient name in English, assign it to the most appropriate category.

            Available categories: $categories

            Return ONLY this JSON: {"category": "<category_name>"}

            Rules:
            - Use exactly one of the available category names (as-is, no modifications)
            - Choose the most specific and accurate category
            - If none fit well, use "other"
        """.trimIndent()
    }
}
