package com.ttfeed.service

import com.ttfeed.database.Standings
import com.ttfeed.database.Teams
import com.ttfeed.model.StandingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object StandingsService {

    suspend fun getForGroup(groupId: String): List<StandingResponse>? {
        val uuid = runCatching { UUID.fromString(groupId) }.getOrNull() ?: return null

        return withContext(Dispatchers.IO) {
            transaction {
                (Standings innerJoin Teams)
                    .select(
                        Standings.teamId,
                        Teams.name,
                        Standings.position,
                        Standings.played,
                        Standings.won,
                        Standings.drawn,
                        Standings.lost,
                        Standings.gamesFor,
                        Standings.gamesAgainst,
                        Standings.points
                    )
                    .where { Standings.groupId eq uuid }
                    .orderBy(Standings.position to SortOrder.ASC)
                    .map { it.toStandingResponse() }
            }
        }
    }

    private fun ResultRow.toStandingResponse() = StandingResponse(
        teamId    = this[Standings.teamId].toString(),
        team      = this[Teams.name],
        position  = this[Standings.position].toInt(),
        played    = this[Standings.played].toInt(),
        won       = this[Standings.won].toInt(),
        drawn     = this[Standings.drawn].toInt(),
        lost      = this[Standings.lost].toInt(),
        gamesWon  = this[Standings.gamesFor].toInt(),
        gamesLost = this[Standings.gamesAgainst].toInt(),
        points    = this[Standings.points].toInt(),
    )
}