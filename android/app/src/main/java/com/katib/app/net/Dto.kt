package com.katib.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the proxy's POST /correct contract. */

@Serializable
data class CorrectRequest(
    val text: String,
    val mode: String,          // "msa" | "gulf"
    val task: String,          // "correct" | "suggest"
)

@Serializable
data class Correction(
    val original: String,
    val corrected: String,
    val start: Int = -1,
    val length: Int = 0,
    val type: String = "other",
    val reason: String = "",
)

@Serializable
data class SuggestionOption(
    val text: String,
    val register: String = "فصحى",
    val rank: Int = 1,
)

@Serializable
data class SuggestionGroup(
    val target: String = "",
    val start: Int = -1,
    val length: Int = 0,
    val options: List<SuggestionOption> = emptyList(),
)

@Serializable
data class CorrectResponse(
    val corrections: List<Correction> = emptyList(),
    val suggestions: List<SuggestionGroup> = emptyList(),
    val mode: String = "msa",
    val task: String = "correct",
    val cached: Boolean = false,
)
