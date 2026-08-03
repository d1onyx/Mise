package com.dishlab.application.service

import com.dishlab.domain.error.ValidationError

enum class VoiceCommandType {
    TIMER_START,
    TIMER_ADD,
    TIMER_STOP,
    TIMER_PAUSE,
    TIMER_PAUSE_INDEX,
    TIMER_RESUME,
    TIMER_RESUME_INDEX,
    TIMER_DELETE,
    TIMER_DELETE_INDEX,
    SESSION_START,
    SESSION_COMPLETE,
    SESSION_NEXT_STEP,
    SESSION_PREVIOUS_STEP,
    SESSION_MUTE,
    SESSION_MIC_MUTE,
    SESSION_REPEAT,
    SESSION_PAUSE,
    UNKNOWN,
}

data class VoiceCommandResult(
    val command: VoiceCommandType,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: String = "high",
)

interface AiCommandProvider {
    suspend fun interpret(text: String, context: Map<String, String>): List<VoiceCommandResult>
}

class FakeAiCommandProvider : AiCommandProvider {
    override suspend fun interpret(text: String, context: Map<String, String>): List<VoiceCommandResult> =
        listOf(VoiceCommandResult(command = VoiceCommandType.UNKNOWN))
}

class AiCommandService(
    private val provider: AiCommandProvider,
) {
    suspend fun interpret(text: String, context: Map<String, String> = emptyMap()): List<VoiceCommandResult> {
        if (text.isBlank()) throw ValidationError(details = mapOf("text" to "Text is required"))
        if (text.length > 1000) throw ValidationError(details = mapOf("text" to "Text must not exceed 1000 characters"))
        return provider.interpret(text.trim(), context)
    }
}
