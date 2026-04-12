package com.ttfeed.scraper.knob

object MatchStatus {
    const val SCHEDULED = "scheduled"
    const val COMPLETED = "completed"
}

object GameType {
    const val SINGLES = "singles"
    const val DOUBLES = "doubles"
}

object GameResult {
    const val HOME       = "home"
    const val AWAY       = "away"
    const val NOT_PLAYED = "not_played"
}

/** Prefix used as a placeholder licence number until a real STT licence is scraped. */
const val PLACEHOLDER_LICENCE_PREFIX = "knob:"

/**
 * Maps league names to their knob.ch rvid parameter.
 * STT has no rvid (null) — it is the default national league.
 * Used by both GroupScraper and MatchDetailScraper.
 */
val FEDERATION_RVIDS: Map<String, Int?> = mapOf(
    "STT"   to null,
    "AGTT"  to 1,
    "ANJTT" to 2,
    "ATTT"  to 3,
    "AVVF"  to 4,
    "MTTV"  to 5,
    "NWTTV" to 6,
    "OTTV"  to 7,
    "TTVI"  to 8
)