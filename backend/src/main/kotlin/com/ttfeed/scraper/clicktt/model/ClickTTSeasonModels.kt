package com.ttfeed.scraper.clicktt.model

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.model.MatchStatus

/**
 * A single group link parsed from the click-tt league overview page.
 * One entry per group (not per division — divisions can contain multiple groups).
 */
data class ParsedClickTTGroup(
    val groupId: Int,
    val championship: String,   // e.g. "MTTV 25/26"
    val divisionName: String,   // e.g. "HE 1. Liga"
    val category: String        // e.g. "Herren", "Damen", "Senioren O40"
)

/**
 * One row from the click-tt group standings table.
 */
data class ParsedClickTTStanding(
    val teamName: String,
    val teamTableId: Int,       // from teamPortrait?teamtable= href — globally unique in click-tt
    val position: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val gamesFor: Int,
    val gamesAgainst: Int,
    val points: Int,
    val isPromotion: Boolean,
    val isRelegation: Boolean
)

/**
 * One match row parsed from the click-tt Spielplan (meeting schedule).
 * meetingId is null for matches that haven't been played yet.
 */
data class ParsedClickTTMatch(
    val meetingId: Int?,
    val date: String,           // "27.08.2025"
    val time: String?,          // "20:15", null if not listed
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: MatchStatus
)

/**
 * Individual set score within a match detail game.
 */
data class ParsedClickTTSet(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int
)

/**
 * One game (singles or doubles) parsed from a click-tt match detail page.
 * Doubles have two person IDs per side; singles have one (the *2 fields are null).
 * Names are in click-tt format: "Lastname, Firstname".
 */
data class ParsedClickTTGame(
    val orderInMatch: Int,
    val gameType: GameType,
    // Home side
    val homePersonId: Int?,
    val homeName: String?,
    val homeKlass: String?,
    val homePersonId2: Int?,    // doubles player 2 only
    val homeName2: String?,
    // Away side
    val awayPersonId: Int?,
    val awayName: String?,
    val awayKlass: String?,
    val awayPersonId2: Int?,    // doubles player 2 only
    val awayName2: String?,
    // Result
    val homeSets: Int?,
    val awaySets: Int?,
    val result: GameResult,
    val sets: List<ParsedClickTTSet>
)

data class ParsedClickTTMatchDetail(
    val meetingId: Int,
    val games: List<ParsedClickTTGame>
)
