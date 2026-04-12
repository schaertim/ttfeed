package com.ttfeed.service

import com.ttfeed.database.Games
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

object GameService {

    suspend fun gameExists(playerId: UUID, playedAt: OffsetDateTime, competition: String): Boolean {
        return withContext(Dispatchers.IO) {
            transaction {
                Games.select(Games.id)
                    .where {
                        (Games.homePlayer1Id eq playerId) and
                                (Games.playedAt eq playedAt) and
                                (Games.competitionName eq competition)
                    }.count() > 0
            }
        }
    }

    suspend fun getPendingKnobMatches(): List<String> {
        return withContext(Dispatchers.IO) {
            transaction {
                val now = OffsetDateTime.now(ZoneId.of("Europe/Zurich"))
                val twoWeeksAgo = now.minusDays(14)

                Games.select(Games.matchId)
                    .where {
                        (Games.matchId.isNotNull()) and
                                (Games.result.isNull()) and
                                (Games.playedAt less now) and
                                (Games.playedAt greaterEq twoWeeksAgo)
                    }
                    .map { it[Games.matchId]!!.toString() }
            }
        }
    }

    suspend fun insertTournamentGame(
        playerId: UUID,
        opponentId: UUID?,
        playedAt: OffsetDateTime,
        competition: String,
        eloDelta: Double?,
        result: String
    ) {
        withContext(Dispatchers.IO) {
            transaction {
                Games.insert {
                    it[this.matchId] = null
                    it[this.homePlayer1Id] = playerId
                    it[this.awayPlayer1Id] = opponentId
                    it[this.playedAt] = playedAt
                    it[this.competitionName] = competition
                    it[this.homePlayer1EloDelta] = eloDelta
                    it[this.awayPlayer1EloDelta] = eloDelta?.let { delta -> -delta }
                    it[this.result] = result
                }
            }
        }
    }
}