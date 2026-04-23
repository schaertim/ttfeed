package com.ttfeed.service

import com.ttfeed.database.*
import com.ttfeed.model.*
import com.ttfeed.util.normalizeClickTtName
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

object PlayerService {

    suspend fun getById(playerId: String): PlayerResponse? {
        val uuid = playerId.toUuidOrNull() ?: return null
        return dbQuery {
            val playerRow = Players.select(Players.id, Players.fullName, Players.licenceNr)
                .where { Players.id eq uuid }
                .firstOrNull() ?: return@dbQuery null

            val seasonData = (PlayerSeasons innerJoin Teams innerJoin Clubs innerJoin Seasons)
                .select(PlayerSeasons.klass, Clubs.name)
                .where { PlayerSeasons.playerId eq uuid }
                .orderBy(Seasons.name to SortOrder.DESC)
                .firstOrNull()

            val currentElo = PlayerElos
                .select(PlayerElos.eloValue)
                .where { PlayerElos.playerId eq uuid }
                .orderBy(PlayerElos.recordedAt to SortOrder.DESC)
                .firstOrNull()
                ?.get(PlayerElos.eloValue)

            playerRow.toPlayerResponse(
                currentClubName = seasonData?.get(Clubs.name),
                klass           = seasonData?.get(PlayerSeasons.klass),
                currentElo      = currentElo,
            )
        }
    }

    suspend fun getEloHistory(playerId: String): List<EloEntryResponse>? {
        val uuid = playerId.toUuidOrNull() ?: return null
        return dbQuery {
            (PlayerElos innerJoin Seasons)
                .select(PlayerElos.eloValue, PlayerElos.recordedAt, Seasons.name)
                .where { PlayerElos.playerId eq uuid }
                .orderBy(PlayerElos.recordedAt to SortOrder.ASC)
                .map {
                    EloEntryResponse(
                        eloValue   = it[PlayerElos.eloValue],
                        recordedAt = it[PlayerElos.recordedAt].toString(),
                        seasonName = it[Seasons.name],
                    )
                }
        }
    }

    suspend fun getMatchHistory(playerId: String): List<PlayerGameResponse>? {
        val uuid = playerId.toUuidOrNull() ?: return null
        return dbQuery {
            val homeTeam   = Teams.alias("home_team")
            val awayTeam   = Teams.alias("away_team")
            val homePlayer = Players.alias("home_player")
            val awayPlayer = Players.alias("away_player")

            Games
                .join(Matches,    JoinType.INNER, Games.matchId,       Matches.id)
                .join(homeTeam,   JoinType.INNER, Matches.homeTeamId,  homeTeam[Teams.id])
                .join(awayTeam,   JoinType.INNER, Matches.awayTeamId,  awayTeam[Teams.id])
                .join(homePlayer, JoinType.LEFT,  Games.homePlayer1Id, homePlayer[Players.id])
                .join(awayPlayer, JoinType.LEFT,  Games.awayPlayer1Id, awayPlayer[Players.id])
                .select(
                    Games.id,
                    Games.homePlayer1Id,
                    Games.homeSets,
                    Games.awaySets,
                    Games.result,
                    Games.homePlayer1EloDelta,
                    Games.awayPlayer1EloDelta,
                    Matches.id,
                    Matches.playedAt,
                    Matches.homeScore,
                    Matches.awayScore,
                    Matches.round,
                    Matches.status,
                    homeTeam[Teams.name],
                    awayTeam[Teams.name],
                    homePlayer[Players.fullName],
                    awayPlayer[Players.fullName],
                )
                .where {
                    ((Games.homePlayer1Id eq uuid) or (Games.awayPlayer1Id eq uuid)) and
                    (Games.gameType eq GameType.SINGLES)
                }
                .orderBy(Matches.playedAt to SortOrder.DESC)
                .map { row ->
                    val isHome = row[Games.homePlayer1Id] == uuid
                    PlayerGameResponse(
                        matchId      = row[Matches.id].toString(),
                        gameId       = row[Games.id].toString(),
                        playedAt     = row[Matches.playedAt]?.toString(),
                        homeTeam     = row[homeTeam[Teams.name]],
                        awayTeam     = row[awayTeam[Teams.name]],
                        homeScore    = row[Matches.homeScore]?.toInt(),
                        awayScore    = row[Matches.awayScore]?.toInt(),
                        round        = row[Matches.round],
                        status       = row[Matches.status],
                        playerSide   = if (isHome) "home" else "away",
                        opponentName = if (isHome) row[awayPlayer[Players.fullName]]
                                       else        row[homePlayer[Players.fullName]],
                        homeSets     = row[Games.homeSets]?.toInt(),
                        awaySets     = row[Games.awaySets]?.toInt(),
                        result       = row[Games.result],
                        eloDelta     = if (isHome) row[Games.homePlayer1EloDelta]
                                       else        row[Games.awayPlayer1EloDelta],
                    )
                }
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
            .where { Players.licenceNr.isNotNull() }
            .map { it[Players.id] to it[Players.licenceNr]!! }
    }

    suspend fun getAllPlayersWithClickTtId(): List<Pair<UUID, Int>> = dbQuery {
        Players.select(Players.id, Players.clickttId)
            .where { Players.clickttId.isNotNull() }
            .map { it[Players.id] to it[Players.clickttId]!! }
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
        val fullName = normalizeClickTtName(clickTtName).lowercase()

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
                Players.clickttId.isNull() and Players.licenceNr.isNotNull()
            }
            .map { it[Players.id] to it[Players.licenceNr]!! }
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

    /** Updates both the click-tt person ID and the display name for a batch of players by licence. */
    suspend fun updateClickTtDataBatch(updates: Map<String, Pair<Int, String>>) {
        if (updates.isEmpty()) return
        dbQuery {
            for ((licence, data) in updates) {
                val (personId, fullName) = data
                Players.update({ Players.licenceNr eq licence }) {
                    it[Players.clickttId] = personId
                    it[Players.fullName]  = fullName
                }
            }
        }
    }

    /**
     * Finds the club that the majority of the given licensed players belong to.
     * Used to match a click-tt club ID to an existing club row scraped from knob.
     */
    suspend fun findClubIdByLicences(licences: List<String>): UUID? {
        if (licences.isEmpty()) return null
        return dbQuery {
            val clubCount = Clubs.id.count()
            Players
                .innerJoin(PlayerSeasons, { Players.id }, { PlayerSeasons.playerId })
                .innerJoin(Teams, { PlayerSeasons.teamId }, { Teams.id })
                .innerJoin(Clubs, { Teams.clubId }, { Clubs.id })
                .select(Clubs.id, clubCount)
                .where { Players.licenceNr inList licences }
                .groupBy(Clubs.id)
                .orderBy(clubCount to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.get(Clubs.id)
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
