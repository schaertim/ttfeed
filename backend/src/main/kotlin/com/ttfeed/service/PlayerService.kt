package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.PagedResponse
import com.ttfeed.model.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.LikePattern
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PlayerService {

    suspend fun getById(playerId: String): PlayerResponse? {
        val uuid = runCatching { UUID.fromString(playerId) }.getOrNull() ?: return null

        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.id, Players.fullName, Players.licenceNr)
                    .where { Players.id eq uuid }
                    .firstOrNull()
                    ?.toPlayerResponse()
            }
        }
    }

    suspend fun search(name: String, page: Int, size: Int): PagedResponse<PlayerResponse>? {
        if (name.length < 3) return null

        return withContext(Dispatchers.IO) {
            transaction {
                val pattern = LikePattern("%${name.lowercase()}%")

                val total = Players.select(Players.id)
                    .where { Players.fullName.lowerCase() like pattern }
                    .count()

                // STEP 1: Fetch the core players first (Ensures Sergey is found!)
                val basePlayers = Players.select(Players.id, Players.fullName, Players.licenceNr)
                    .where { Players.fullName.lowerCase() like pattern }
                    .orderBy(Players.fullName to SortOrder.ASC)
                    .limit(size).offset(start = (page * size).toLong()).toList()

                val playerIds = basePlayers.map { it[Players.id] }

                if (playerIds.isEmpty()) {
                    return@transaction PagedResponse(emptyList(), page, size, total)
                }

                // STEP 2: Fetch optional stats ONLY for the players we found
                val playerStats = (PlayerSeasons innerJoin Teams innerJoin Clubs innerJoin Seasons)
                    .select(PlayerSeasons.playerId, PlayerSeasons.klass, Clubs.name)
                    .where { PlayerSeasons.playerId inList playerIds }
                    .orderBy(Seasons.name to SortOrder.DESC) // Get newest season first
                    .toList()
                    .groupBy { it[PlayerSeasons.playerId] }
                    .mapValues { it.value.first() }

                // STEP 3: Map them together. If stats are missing, they safely default to null.
                val items = basePlayers.map { row ->
                    val pId = row[Players.id]
                    val stats = playerStats[pId]

                    row.toPlayerResponse(
                        currentClubName = stats?.get(Clubs.name),
                        klass = stats?.get(PlayerSeasons.klass),
                        currentElo = null // Mocking until click-tt scraper is done
                    )
                }

                PagedResponse(
                    items = items,
                    page = page,
                    size = size,
                    total = total
                )
            }
        }
    }

    // Centralized mapper to keep things consistent with your other services
    private fun ResultRow.toPlayerResponse(
        currentClubName: String? = null,
        klass: String? = null,
        currentElo: Int? = null
    ) = PlayerResponse(
        id              = this[Players.id].toString(),
        fullName        = this[Players.fullName],
        licenceNr       = this[Players.licenceNr],
        currentClubName = currentClubName,
        klass           = klass,
        currentElo      = currentElo
    )
}