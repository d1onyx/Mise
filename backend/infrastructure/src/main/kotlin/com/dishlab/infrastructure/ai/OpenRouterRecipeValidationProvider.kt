package com.dishlab.infrastructure.ai

import com.dishlab.application.service.RecipeValidationCheck
import com.dishlab.application.service.RecipeValidationProvider
import com.dishlab.application.service.RecipeValidationResult
import com.dishlab.application.service.RecipeValidationSeverity
import com.dishlab.domain.model.RecipeVersion
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

class OpenRouterRecipeValidationProvider(
    private val apiKey: String,
    private val model: String = "google/gemma-4-31b-it:free",
) : RecipeValidationProvider {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = ConcurrentHashMap<String, RecipeValidationResult>()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun validate(recipe: RecipeVersion): RecipeValidationResult = runCatching {
        val input = recipe.toInput()
        val cacheKey = json.encodeToString(ValidationInput.serializer(), input)
        cache[cacheKey]?.let { return it }
        val httpResponse = client.post(OPENROUTER_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://dishlab.app")
            header("X-Title", "DishLab Recipe Validator")
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", SYSTEM_PROMPT),
                        ChatMessage("user", json.encodeToString(ValidationInput.serializer(), input)),
                    ),
                ),
            )
        }
        if (httpResponse.status != HttpStatusCode.OK) return unavailableResult()
        val response: ChatResponse = httpResponse.body()

        val content = response.choices.firstOrNull()?.message?.content.orEmpty().extractJson()
        if (content.isBlank()) return unavailableResult()
        val echo = runCatching { json.decodeFromString<ValidationEcho>(content) }.getOrNull()
            ?: return unavailableResult()
        val invalidFields = detectInvalidFields(input, echo)
        RecipeValidationResult(
            valid = invalidFields.isEmpty(),
            checks = invalidFields.map { field ->
                RecipeValidationCheck(
                    field = field,
                    severity = RecipeValidationSeverity.ERROR,
                    message = "",
                    conflictsWith = null,
                )
            },
        ).also { cache[cacheKey] = it }
    }.getOrElse { unavailableResult() }

    private fun unavailableResult(): RecipeValidationResult =
        RecipeValidationResult(valid = true, checks = emptyList())

    private fun detectInvalidFields(input: ValidationInput, echo: ValidationEcho): List<String> {
        val invalid = mutableListOf<String>()
        if (echo.title == null) invalid.add("title")
        if (echo.servings == null) invalid.add("servings")
        input.ingredients.forEachIndexed { i, ing ->
            val out = echo.ingredients.getOrNull(i)
            if (out == null) {
                invalid.add("ingredients[$i]")
            } else {
                if (out.name == null && ing.name.isNotBlank()) invalid.add("ingredients[$i].name")
                if (out.amount == null) invalid.add("ingredients[$i].amount")
            }
        }
        input.steps.forEachIndexed { i, step ->
            val out = echo.steps.getOrNull(i)
            if (out == null || (out.text == null && step.text.isNotBlank())) invalid.add("steps[$i]")
        }
        return invalid
    }

    private fun RecipeVersion.toInput() = ValidationInput(
        title = title,
        servings = servings,
        ingredients = ingredients.map { IngredientInput(it.name, it.amount, it.unit) },
        steps = steps.sortedBy { it.position }.map { StepInput(it.text) },
    )

    private fun String.extractJson(): String {
        val trimmed = trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed.substringAfter('\n').substringBeforeLast("```").trim()
    }

    @Serializable
    private data class ChatMessage(val role: String, val content: String? = null)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float = 0.0f,
        @SerialName("max_tokens") val maxTokens: Int = 1_500,
        @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    )

    @Serializable
    private data class ResponseFormat(val type: String = "json_object")

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class ValidationInput(
        val title: String,
        val servings: Int,
        val ingredients: List<IngredientInput>,
        val steps: List<StepInput>,
    )

    @Serializable
    private data class IngredientInput(
        val name: String,
        val amount: Double,
        val unit: String,
    )

    @Serializable
    private data class StepInput(
        val text: String,
    )

    @Serializable
    private data class ValidationEcho(
        val title: String? = null,
        val servings: Int? = null,
        val ingredients: List<IngredientEcho?> = emptyList(),
        val steps: List<StepEcho?> = emptyList(),
    )

    @Serializable
    private data class IngredientEcho(
        val name: String? = null,
        val amount: Double? = null,
        val unit: String? = null,
    )

    @Serializable
    private data class StepEcho(
        val text: String? = null,
    )

    companion object {
        private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

        val SYSTEM_PROMPT = """
            You are a strict recipe validator. You receive a recipe JSON object.
            Return the EXACT same JSON structure with the same keys.

            STRICTNESS RULE: When in doubt, set the field to null. Only keep a value if you are confident it is genuinely valid.

            Title rules (set to null if ANY of these apply):
            - Not a recognizable food or drink dish name
            - Random letters, gibberish, or keyboard mashing
            - A non-food object, material, or concept (e.g. "plastic clay", "table", "hello world")
            - A single generic word that is not a dish (e.g. "food", "recipe", "test")
            - Promotional text, instructions, or questions

            Ingredient name rules (set the whole ingredient object to null if ANY apply):
            - Not a food, drink, spice, herb, condiment, or cooking ingredient
            - Gibberish or random characters
            - A non-food object or material
            - A brand name with no recognizable food base

            Ingredient amount rules (set amount to null if):
            - Zero or negative
            - Impossibly large (e.g. 10000 kg of salt)

            Step rules (set the whole step object to null if):
            - Not a cooking or food preparation instruction
            - Gibberish or random characters
            - Instructions unrelated to preparing food

            Other rules:
            - Do NOT add explanation, message, or extra fields.
            - Return ONLY the JSON object. No markdown, no commentary.

            Example input:
            {"title":"Борщ","servings":4,"ingredients":[{"name":"буряк","amount":300,"unit":"г"},{"name":"пластмасова глина","amount":1,"unit":"шт"}],"steps":[{"text":"Нарізати буряк"},{"text":"абвгдеж"}]}

            Example output (second ingredient is not food, second step is gibberish):
            {"title":"Борщ","servings":4,"ingredients":[{"name":"буряк","amount":300,"unit":"г"},null],"steps":[{"text":"Нарізати буряк"},null]}

            Example input:
            {"title":"тест123","servings":2,"ingredients":[{"name":"яйце","amount":2,"unit":"шт"}],"steps":[{"text":"Зварити яйця"}]}

            Example output (title is not a dish name):
            {"title":null,"servings":2,"ingredients":[{"name":"яйце","amount":2,"unit":"шт"}],"steps":[{"text":"Зварити яйця"}]}
        """.trimIndent()
    }
}
