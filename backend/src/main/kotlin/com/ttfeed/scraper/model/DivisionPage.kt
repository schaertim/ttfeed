package com.ttfeed.scraper.model

data class ParsedTeam(
    val name: String,
    val knobClubId: Int,
    val knobTeamId: Int
)

data class ParsedPlayer(
    val fullName: String,
    val knobId: Int,
    val klass: String,
    val knobClubId: Int,
    val knobTeamId: Int
)

data class ParsedMatch(
    val knobMatchId: Int,
    val round: String?,
    val homeKnobTeamId: Int,
    val awayKnobTeamId: Int,
    val playedAt: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String
)

data class ParsedDivisionPage(
    val teams: List<ParsedTeam>,
    val players: List<ParsedPlayer>,
    val matches: List<ParsedMatch>,
    val standings: List<ParsedStandingRow>,
    val promotionSpots: Int,
    val relegationSpots: Int
)

data class ParsedStandingRow(
    val position: Int,
    val knobTeamId: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val gamesFor: Int,
    val gamesAgainst: Int,
    val points: Int
)

data class ParsedStandingsPage(
    val standings: List<ParsedStandingRow>,
    val promotionSpots: Int,
    val relegationSpots: Int
)

data class ParsedLicensedPlayer(
    val fullName: String,
    val licenceNr: String
)