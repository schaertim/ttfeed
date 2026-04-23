package com.ttfeed.service

import com.ttfeed.database.Games
import com.ttfeed.database.dbQuery
import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.update
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

    /**
     * Finds an existing league game (matchId IS NOT NULL) for the given player on the given day
     * and fills in their ELO delta. Tries the player as home first, then as away.
     * Returns true if a game was found and updated.
     */
    suspend fun updateLeagueGameEloDelta(
        playerId: UUID,
        opponentId: UUID?,
        dayStart: OffsetDateTime,
        eloDelta: Double
    ): Boolean = dbQuery {
        val dayEnd = dayStart.plusDays(1)

        val homeBase = (Games.matchId.isNotNull()) and
            (Games.homePlayer1Id eq playerId) and
            (Games.playedAt greaterEq dayStart) and
            (Games.playedAt less dayEnd) and
            (Games.homePlayer1EloDelta.isNull())
        val homeCondition = if (opponentId != null)
            homeBase and (Games.awayPlayer1Id eq opponentId) else homeBase

        val homeUpdate = Games.update({ homeCondition }) {
            it[Games.homePlayer1EloDelta] = eloDelta
        }
        if (homeUpdate > 0) return@dbQuery true

        val awayBase = (Games.matchId.isNotNull()) and
            (Games.awayPlayer1Id eq playerId) and
            (Games.playedAt greaterEq dayStart) and
            (Games.playedAt less dayEnd) and
            (Games.awayPlayer1EloDelta.isNull())
        val awayCondition = if (opponentId != null)
            awayBase and (Games.homePlayer1Id eq opponentId) else awayBase

        val awayUpdate = Games.update({ awayCondition }) {
            it[Games.awayPlayer1EloDelta] = eloDelta
        }
        awayUpdate > 0
    }

    suspend fun getPlayerIdsFromMatches(matchIds: Set<UUID>): Set<UUID> = dbQuery {
        if (matchIds.isEmpty()) return@dbQuery emptySet()
        Games.select(Games.homePlayer1Id, Games.awayPlayer1Id)
            .where { Games.matchId inList matchIds }
            .flatMap { listOfNotNull(it[Games.homePlayer1Id], it[Games.awayPlayer1Id]) }
            .toSet()
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
