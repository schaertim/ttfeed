package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.PagedResponse
import com.ttfeed.model.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
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

    suspend fun getAllLicensedPlayers(): List<Pair<UUID, String>> {
        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.id, Players.licenceNr)
                    .where { Players.licenceNr.isNotNull() and (Players.licenceNr notLike "knob:%") }
                    .map { Pair(it[Players.id], it[Players.licenceNr]!!) }
            }
        }
    }

    suspend fun saveBaseElo(playerId: UUID, seasonId: UUID, eloValue: Int) {
        withContext(Dispatchers.IO) {
            transaction {
                PlayerElos.insert {
                    it[this.playerId] = playerId
                    it[this.seasonId] = seasonId
                    it[this.eloValue] = eloValue
                    it[this.recordedAt] = java.time.OffsetDateTime.now(java.time.ZoneId.of("Europe/Zurich"))
                }
            }
        }
    }

    suspend fun findPlayerIdByName(clickTtName: String): UUID? {
        val parts = clickTtName.split(",")

        // Baut aus "Nachname, Vorname" exakt "Nachname Vorname"
        val expectedFullName = if (parts.size >= 2) {
            val lastName = parts[0].trim()
            val firstName = parts[1].trim()
            "$lastName $firstName".lowercase()
        } else {
            clickTtName.trim().lowercase()
        }

        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.id)
                    .where { Players.fullName.lowerCase() eq expectedFullName }
                    .map { it[Players.id] }
                    .firstOrNull()
            }
        }
    }

    suspend fun getLicenceNrById(playerId: UUID): String? {
        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.licenceNr)
                    .where { Players.id eq playerId }
                    .map { it[Players.licenceNr] }
                    .firstOrNull()
            }
        }
    }

    suspend fun getPlayersMissingClickTtId(): List<Pair<UUID, String>> {
        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.id, Players.licenceNr)
                    .where {
                        (Players.clickttId.isNull()) and
                                (Players.licenceNr notLike "knob:%") // Keine Knob-Dummys
                    }
                    .map { it[Players.id] to it[Players.licenceNr] }
            }
        }
    }

    suspend fun updateClickTtIdsBatch(mappings: Map<String, Int>) {
        if (mappings.isEmpty()) return

        withContext(Dispatchers.IO) {
            transaction {
                // Wir iterieren über unsere gefundenen Paare und updaten die DB
                for ((licence, personId) in mappings) {
                    Players.update({ Players.licenceNr eq licence }) {
                        it[this.clickttId] = personId
                    }
                }
            }
        }
    }

    suspend fun getClickTtIdById(playerId: UUID): Int? {
        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.clickttId)
                    .where { Players.id eq playerId }
                    .map { it[Players.clickttId] }
                    .firstOrNull()
            }
        }
    }

    private fun ResultRow.toPlayerResponse(
        currentClubName: String? = null,
        klass: String? = null,
        currentElo: Int? = null,
        isSyncing: Boolean = false
    ) = PlayerResponse(
        id              = this[Players.id].toString(),
        fullName        = this[Players.fullName],
        licenceNr       = this[Players.licenceNr],
        currentClubName = currentClubName,
        klass           = klass,
        currentElo      = currentElo,
        isSyncing       = isSyncing
    )
}