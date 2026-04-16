package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.PagedResponse
import com.ttfeed.model.PlayerResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

object PlayerService {

    suspend fun getById(playerId: String): PlayerResponse? {
        val uuid = playerId.toUuidOrNull() ?: return null
        return dbQuery {
            Players.select(Players.id, Players.fullName, Players.licenceNr)
                .where { Players.id eq uuid }
                .firstOrNull()
                ?.toPlayerResponse()
        }
    }

    suspend fun search(name: String, page: Int, size: Int): PagedResponse<PlayerResponse>? {
        if (name.length < 3) return null

        return dbQuery {
            val pattern = LikePattern("%${name.lowercase()}%")

            val total = Players.select(Players.id)
                .where { Players.fullName.lowerCase() like pattern }
                .count()

            // Step 1 — fetch paged core player rows
            val basePlayers = Players.select(Players.id, Players.fullName, Players.licenceNr)
                .where { Players.fullName.lowerCase() like pattern }
                .orderBy(Players.fullName to SortOrder.ASC)
                .limit(size).offset(start = (page * size).toLong())
                .toList()

            val playerIds = basePlayers.map { it[Players.id] }

            if (playerIds.isEmpty()) {
                return@dbQuery PagedResponse(emptyList(), page, size, total)
            }

            // Step 2 — fetch club/klass for found players, picking the most recent season
            val playerStats = (PlayerSeasons innerJoin Teams innerJoin Clubs innerJoin Seasons)
                .select(PlayerSeasons.playerId, PlayerSeasons.klass, Clubs.name)
                .where { PlayerSeasons.playerId inList playerIds }
                .orderBy(Seasons.name to SortOrder.DESC)
                .toList()
                .groupBy { it[PlayerSeasons.playerId] }
                .mapValues { it.value.first() }

            // Step 3 — merge results
            val items = basePlayers.map { row ->
                val stats = playerStats[row[Players.id]]
                row.toPlayerResponse(
                    currentClubName = stats?.get(Clubs.name),
                    klass           = stats?.get(PlayerSeasons.klass),
                )
            }

            PagedResponse(items = items, page = page, size = size, total = total)
        }
    }

    suspend fun getAllLicensedPlayers(): List<Pair<UUID, String>> = dbQuery {
        Players.select(Players.id, Players.licenceNr)
            .where { Players.licenceNr notLike "knob:%" }
            .map { it[Players.id] to it[Players.licenceNr] }
    }

    suspend fun saveBaseElo(playerId: UUID, seasonId: UUID, eloValue: Int) = dbQuery {
        PlayerElos.insert {
            it[PlayerElos.playerId]   = playerId
            it[PlayerElos.seasonId]   = seasonId
            it[PlayerElos.eloValue]   = eloValue
            it[PlayerElos.recordedAt] = OffsetDateTime.now(ZoneId.of("Europe/Zurich"))
        }
    }

    suspend fun findPlayerIdByName(clickTtName: String): UUID? {
        // click-tt names are formatted as "Lastname, Firstname" — normalise to "Lastname Firstname"
        val parts = clickTtName.split(",")
        val fullName = if (parts.size >= 2) {
            "${parts[0].trim()} ${parts[1].trim()}".lowercase()
        } else {
            clickTtName.trim().lowercase()
        }

        return dbQuery {
            Players.select(Players.id)
                .where { Players.fullName.lowerCase() eq fullName }
                .map { it[Players.id] }
                .firstOrNull()
        }
    }

    suspend fun getLicenceNrById(playerId: UUID): String? = dbQuery {
        Players.select(Players.licenceNr)
            .where { Players.id eq playerId }
            .map { it[Players.licenceNr] }
            .firstOrNull()
    }

    suspend fun getPlayersMissingClickTtId(): List<Pair<UUID, String>> = dbQuery {
        Players.select(Players.id, Players.licenceNr)
            .where {
                Players.clickttId.isNull() and (Players.licenceNr notLike "knob:%")
            }
            .map { it[Players.id] to it[Players.licenceNr] }
    }

    suspend fun updateClickTtIdsBatch(mappings: Map<String, Int>) {
        if (mappings.isEmpty()) return
        dbQuery {
            for ((licence, personId) in mappings) {
                Players.update({ Players.licenceNr eq licence }) {
                    it[Players.clickttId] = personId
                }
            }
        }
    }

    suspend fun getClickTtIdById(playerId: UUID): Int? = dbQuery {
        Players.select(Players.clickttId)
            .where { Players.id eq playerId }
            .map { it[Players.clickttId] }
            .firstOrNull()
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
