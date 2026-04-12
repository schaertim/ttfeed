package com.ttfeed.scraper.knob.model

data class ParsedSet(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int
)

data class ParsedGame(
    val orderInMatch: Int,
    val gameType: String,
    val homePlayer1KnobId: Int?,
    val homePlayer2KnobId: Int?,
    val awayPlayer1KnobId: Int?,
    val awayPlayer2KnobId: Int?,
    val homeSets: Int?,
    val awaySets: Int?,
    val result: String,
    val sets: List<ParsedSet>
)

data class ParsedMatchDetail(
    val knobMatchId: Int,
    val games: List<ParsedGame>
)

data class GruppePageResult(
    val gruppeId: Int,
    val leagueName: String,
    val divisionName: String,
    val groupName: String
)