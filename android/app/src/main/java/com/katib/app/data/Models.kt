package com.katib.app.data

/** Writing mode controls correction rules and accepted vocabulary. */
enum class WritingMode(val wire: String, val arabicLabel: String) {
    MSA("msa", "فصحى"),
    GULF("gulf", "خليجي");

    companion object {
        fun fromWire(value: String?): WritingMode =
            entries.firstOrNull { it.wire == value } ?: MSA
    }
}

/** Snapshot of local usage statistics shown on the dashboard. */
data class WritingStats(
    val correctionsThisWeek: Int = 0,
    val suggestionsAccepted: Int = 0,
    val topErrorType: String = "—",
    val correctionsToday: Int = 0,
)

/** Free-tier limits per the Katib spec. */
object FreeTier {
    const val DAILY_CORRECTION_LIMIT = 30
    const val SUGGESTION_CHIPS = 1
}

object Premium {
    const val SUGGESTION_CHIPS = 3
}
