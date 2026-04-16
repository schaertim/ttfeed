package com.ttfeed.model

enum class GameType {
    SINGLES,
    DOUBLES
}

enum class GameResult {
    HOME,
    AWAY,
    DRAW
}

enum class MatchStatus {
    SCHEDULED, // Geplant, noch nicht gespielt
    COMPLETED,  // Gespielt und gewertet
    WALKOVER   // Forfait (W.O.)
}

enum class GenderCategory {
    MENS,
    WOMENS
}