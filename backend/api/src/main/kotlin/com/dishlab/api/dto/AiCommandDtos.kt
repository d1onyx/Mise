package com.dishlab.api.dto

import com.dishlab.application.service.VoiceCommandResult
import kotlinx.serialization.Serializable

@Serializable
data class InterpretCommandRequest(
    val text: String,
    val context: Map<String, String> = emptyMap(),
)

@Serializable
data class InterpretCommandResponse(
    val command: String,
    val parameters: Map<String, String>,
    val confidence: String,
)

fun VoiceCommandResult.toResponse() = InterpretCommandResponse(
    command = command.name,
    parameters = parameters,
    confidence = confidence,
)

fun List<VoiceCommandResult>.toResponse() = map { it.toResponse() }
