package com.ttfeed.scraper.knob.model

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType

data class ParsedSet(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int
)

data class ParsedGame(
    val orderInMatch: Int,
    val gameType: GameType,
    val homePlayer1KnobId: Int?,
    val homePlayer2KnobId: Int?,
    val awayPlayer1KnobId: Int?,
    val awayPlayer2KnobId: Int?,
    val homeSets: Int?,
    val awaySets: Int?,
    val result: GameResult,
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