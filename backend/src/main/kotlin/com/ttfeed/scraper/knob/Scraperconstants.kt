package com.ttfeed.scraper.knob

/**
 * Maps league names to their knob.ch rvid parameter.
 * STT has no rvid (null) — it is the default national league.
 * Used by both GroupScraper and MatchScraper.
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