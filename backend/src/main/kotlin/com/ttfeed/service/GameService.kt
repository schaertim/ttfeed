package com.ttfeed.service

import com.ttfeed.database.Games
import com.ttfeed.database.dbQuery
import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import java.time.OffsetDateTime
import java.util.*

object GameService {

    suspend fun gameExists(playerId: UUID, playedAt: OffsetDateTime, competition: String): Boolean = dbQuery {
        Games.select(Games.id)
            .where {
                (Games.homePlayer1Id eq playerId) and
                        (Games.playedAt eq playedAt) and
                        (Games.competitionName eq competition)
            }.count() > 0
    }

    suspend fun insertTournamentGame(
        playerId: UUID,
        opponentId: UUID?,
        playedAt: OffsetDateTime,
        competition: String,
        eloDelta: Double?,
        result: GameResult,
        gameType: GameType = GameType.SINGLES
    ) = dbQuery {
        Games.insert {
            it[Games.matchId]             = null
            it[Games.homePlayer1Id]       = playerId
            it[Games.awayPlayer1Id]       = opponentId
            it[Games.playedAt]            = playedAt
            it[Games.competitionName]     = competition
            it[Games.homePlayer1EloDelta] = eloDelta
            it[Games.awayPlayer1EloDelta] = eloDelta?.let { delta -> -delta }
            it[Games.result]              = result
            it[Games.gameType]            = gameType
        }
    }
}
