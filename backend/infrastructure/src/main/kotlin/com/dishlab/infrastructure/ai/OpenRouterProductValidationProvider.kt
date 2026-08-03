package com.dishlab.infrastructure.ai

import com.dishlab.application.service.ProductValidationProvider
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

class OpenRouterProductValidationProvider(
    private val apiKey: String,
    private val model: String = "google/gemma-4-31b-it:free",
) : ProductValidationProvider {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = ConcurrentHashMap<String, Boolean>()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun validate(name: String): Boolean {
        val key = name.trim().lowercase()
        cache[key]?.let { return it }
        val httpResponse = client.post(OPENROUTER_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://dishlab.app")
            header("X-Title", "DishLab Product Validator")
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", SYSTEM_PROMPT),
                        ChatMessage("user", name.trim()),
                    ),
                ),
            )
        }
        if (httpResponse.status != HttpStatusCode.OK) return false
        val response: ChatResponse = httpResponse.body()
        val content = response.choices.firstOrNull()?.message?.content.orEmpty().trim()
        val result = runCatching { json.decodeFromString<ValidationResult>(content) }.getOrNull()
            ?: return false
        val valid = result.valid
        cache[key] = valid
        return valid
    }

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float = 0.0f,
        @SerialName("max_tokens") val maxTokens: Int = 100,
        @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    )

    @Serializable
    private data class ResponseFormat(val type: String = "json_object")

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class ValidationResult(val valid: Boolean = false)

    private companion object {
        const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

        val SYSTEM_PROMPT = """
            You are a strict product name validator for a food pantry app.
            The user submits a product name in ANY language. Decide if it is a real food, drink, spice, herb, condiment, or cooking ingredient.

            Return ONLY this JSON: {"valid": true} or {"valid": false}

            Set valid=false if ANY of these apply:
            - Not a food, drink, or cooking ingredient
            - Profanity, vulgarity, or offensive text in ANY language (e.g. Ukrainian "гівно", English "shit", Russian "дерьмо")
            - Gibberish, random letters, or keyboard mashing
            - A non-food object or material (e.g. "plastic", "table", "clay")
            - A single character or meaningless short string
            - A joke or test input (e.g. "test", "тест", "asdf")

            Set valid=true ONLY if:
            - It is a clearly recognizable food, drink, spice, condiment, or ingredient (in any language)
            - It is a known food brand name
            - It is a dish that can be used as an ingredient (e.g. "tomato sauce", "pesto")

            Examples:
            - "яблуко" → {"valid": true}   (apple in Ukrainian)
            - "молоко" → {"valid": true}    (milk in Ukrainian)
            - "гівно" → {"valid": false}    (Ukrainian profanity, not food)
            - "сіль" → {"valid": true}      (salt in Ukrainian)
            - "test" → {"valid": false}     (not a food)
            - "asdfgh" → {"valid": false}   (gibberish)
            - "tomato" → {"valid": true}
            - "plastic" → {"valid": false}

            When in doubt, set valid=false.
        """.trimIndent()
    }
}
