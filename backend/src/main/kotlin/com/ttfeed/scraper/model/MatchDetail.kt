package com.ttfeed.scraper.model

data class ParsedSet(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int
)

data class ParsedGame(
    val orderInMatch: Int,
    val gameType: String,          // "singles" or "doubles"
    val homePlayer1KnobId: Int?,
    val homePlayer2KnobId: Int?,   // doubles only
    val awayPlayer1KnobId: Int?,
    val awayPlayer2KnobId: Int?,   // doubles only
    val homeSets: Int?,
    val awaySets: Int?,
    val result: String,            // "home", "away", "not_played"
    val sets: List<ParsedSet>
)

data class ParsedMatchDetail(
    val knobMatchId: Int,
    val games: List<ParsedGame>
)