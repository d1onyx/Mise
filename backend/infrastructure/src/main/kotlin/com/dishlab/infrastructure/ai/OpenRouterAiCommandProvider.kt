package com.dishlab.infrastructure.ai

import com.dishlab.application.service.AiCommandProvider
import com.dishlab.application.service.VoiceCommandResult
import com.dishlab.application.service.VoiceCommandType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenRouterAiCommandProvider(
    private val apiKey: String,
    private val model: String = "openai/gpt-oss-20b:free",
) : AiCommandProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val systemPrompt = """
        You are a voice command classifier for a cooking app called DishLab.
        The user speaks in any language (Ukrainian, English, etc.) including indirect or polite forms.
        Your job: identify the USER'S INTENT and map it to the closest command.
        Return ONLY a JSON array of command objects, even if there is only one command:
        [{"command":"<COMMAND_TYPE>","parameters":{},"confidence":"<high|medium|low>"}]
        If the user says multiple things (e.g. two timers), return one object per command in the array.

        Commands, examples and required parameters:

        --- Navigation ---
        SESSION_NEXT_STEP     - go to next step; "далі", "наступний крок", "next", "next step"
        SESSION_PREVIOUS_STEP - go to previous step; "назад", "попередній крок", "back", "previous step"
        SESSION_COMPLETE      - finish cooking; "завершити готування", "готово", "done cooking"
        SESSION_PAUSE         - pause or resume the whole session; "постав на паузу рецепт", "пауза", "pause cooking", "зупини готування"
        SESSION_REPEAT        - repeat current step aloud (TTS); "голос", "звук", "повтори", "прочитай ще раз", "repeat", "read again"
        SESSION_MUTE          - mute TTS voice/speaker output (cannot unmute by voice); "вимкни звук", "вимкни голос", "заглуши", "тихо", "mute", "silence", "turn off voice", "turn off sound"
        SESSION_MIC_MUTE      - mute/disable the microphone input (stop listening); "вимкни мікрофон", "вимкни мій мікрофон", "не слухай", "стоп мікрофон", "turn off microphone", "disable mic", "stop listening"
        SESSION_START         - start cooking; "почати готувати", "start cooking"

        --- Timers (no specific index) ---
        TIMER_START           - start the built-in step timer with NO custom duration; "постав таймер", "запусти таймер кроку", "start step timer"
                                Use ONLY when the user does NOT mention any number of minutes/seconds.
        TIMER_ADD             - add a NEW timer with a SPECIFIC duration the user stated explicitly.
                                Examples: "включи таймер на 25 хвилин", "додай таймер на 10 хв", "хочу таймер на 5 хвилин", "add timer for 15 minutes", "set a 20 minute timer"
                                REQUIRED parameter: {"minutes":"<INTEGER_ONLY>"} — the value MUST be a plain integer string, no units, no spaces (e.g. "10", not "10 minutes").
                                CRITICAL: if the user says ANY number of minutes → use TIMER_ADD, NOT TIMER_START.
        TIMER_PAUSE           - pause the first currently running timer (no index); "призупини таймер", "pause timer"
        TIMER_RESUME          - resume the first paused timer (no index); "відновити таймер", "resume timer"
        TIMER_STOP            - stop/cancel the last timer (no index); "зупини таймер", "stop timer"
        TIMER_DELETE          - delete the last timer (no index); "видали таймер", "delete timer"

        --- Timers by position (1-based index) ---
        CRITICAL: if the user mentions ANY ordinal word (перший/другий/третій/четвертий/first/second/third/fourth/1st/2nd/3rd …)
        you MUST use the *_INDEX variant and include {"index":"<N>"}.
        Never use the non-index variant when an ordinal is present.
        REQUIRED parameter for all three: {"index":"<1-based integer>"}

        TIMER_PAUSE_INDEX     - pause a specific timer by position
                                "постав на паузу перший таймер", "зупини другий таймер", "pause the second timer"
        TIMER_RESUME_INDEX    - resume a specific timer by position
                                "відновити другий таймер", "запусти перший таймер знову", "resume the first timer"
        TIMER_DELETE_INDEX    - delete/stop/turn off a specific timer by position
                                "видалити третій таймер", "вимкни перший таймер", "зупини другий таймер",
                                "delete timer number two", "stop the first timer", "turn off the third timer"

        --- Fallback ---
        UNKNOWN               - completely unrelated to cooking app commands

        Rules:
        - If the user mentions ANY number of minutes/seconds → TIMER_ADD with {"minutes":"<N>"} where N is digits only
        - If the user mentions an ordinal word (перший/другий/first/second/1st/2nd …) → ALWAYS use the *_INDEX variant with {"index":"<N>"}
        - "вимкни", "зупини", "видали", "stop", "turn off", "delete" + ordinal → TIMER_DELETE_INDEX
        - "призупини", "пауза", "pause" + ordinal → TIMER_PAUSE_INDEX
        - "відновити", "продовжи", "resume" + ordinal → TIMER_RESUME_INDEX
        - Never use the plain (non-index) timer commands when an ordinal is present
        - SESSION_MUTE = speaker/TTS output (звук, голос, динамік); SESSION_MIC_MUTE = microphone input (мікрофон, слухання)
        - Never return both TIMER_ADD and TIMER_START for the same utterance
        - Parameter values must ALWAYS be plain integers as strings: "10" not "10 minutes", "1" not "перший"
        - Return ONLY a valid JSON array. No markdown, no explanation. Always wrap in [ ].
    """.trimIndent()

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float = 0.1f,
        @SerialName("max_tokens") val maxTokens: Int = 200,
    )

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class CommandJson(
        val command: String = "UNKNOWN",
        val parameters: Map<String, String> = emptyMap(),
        val confidence: String = "high",
    )

    override suspend fun interpret(text: String, context: Map<String, String>): List<VoiceCommandResult> {
        val userMessage = buildString {
            if (context.isNotEmpty()) {
                append("Context: ")
                append(context.entries.joinToString(", ") { "${it.key}=${it.value}" })
                append("\n")
            }
            append(text)
        }

        return try {
            val response: ChatResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                header("HTTP-Referer", "https://dishlab.app")
                header("X-Title", "DishLab")
                setBody(
                    ChatRequest(
                        model = model,
                        messages = listOf(
                            ChatMessage("system", systemPrompt),
                            ChatMessage("user", userMessage),
                        ),
                    ),
                )
            }.body()

            val rawJson = response.choices.firstOrNull()?.message?.content
                ?: return listOf(VoiceCommandResult(VoiceCommandType.UNKNOWN, confidence = "low"))

            val parsed: List<CommandJson> = runCatching {
                json.decodeFromString<List<CommandJson>>(rawJson)
            }.getOrElse {
                // AI returned a single object instead of an array — wrap it
                runCatching { listOf(json.decodeFromString<CommandJson>(rawJson)) }.getOrDefault(emptyList())
            }

            parsed.map { cmd ->
                VoiceCommandResult(
                    command = runCatching { VoiceCommandType.valueOf(cmd.command) }.getOrDefault(VoiceCommandType.UNKNOWN),
                    parameters = cmd.parameters,
                    confidence = cmd.confidence,
                )
            }.ifEmpty { listOf(VoiceCommandResult(VoiceCommandType.UNKNOWN, confidence = "low")) }
        } catch (e: Exception) {
            listOf(VoiceCommandResult(VoiceCommandType.UNKNOWN, confidence = "low"))
        }
    }
}
