package com.ttfeed.scraper.clicktt

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.service.GameService
import com.ttfeed.service.PlayerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ClickTTSyncService {
    private val logger        = LoggerFactory.getLogger(ClickTTSyncService::class.java)
    private val client        = ClickTTClient()
    private val parser        = ClickTTParser()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val swissZone     = ZoneId.of("Europe/Zurich")

    private val syncCooldowns = ConcurrentHashMap<UUID, Instant>()
    private val COOLDOWN      = Duration.ofMinutes(5)

    /** Syncs ELO and game history for all players with a click-tt ID. Intended for backfill. */
    suspend fun runPortraitBackfill(seasonId: UUID) {
        val players = PlayerService.getAllPlayersWithClickTtId()
        logger.info("Portrait backfill starting — ${players.size} players with click-tt ID")

        var successCount   = 0
        var noEloCount     = 0
        var failCount      = 0
        var tourneyInserts = 0
        var leagueUpdates  = 0

        for ((index, pair) in players.withIndex()) {
            val (playerId, personId) = pair
            try {
                val result = syncPlayerDetailed(playerId, seasonId)
                successCount++
                tourneyInserts += result.tournamentGamesInserted
                leagueUpdates  += result.leagueDeltasUpdated
                if (result.elo == null) noEloCount++
                if (index % 50 == 0) {
                    logger.info(
                        "Portrait backfill progress: $index / ${players.size} — " +
                        "$successCount ok, $noEloCount no-elo, $failCount failed, " +
                        "$tourneyInserts tournament inserts, $leagueUpdates league delta updates"
                    )
                }
            } catch (e: Exception) {
                failCount++
                logger.warn("Portrait backfill failed for player $playerId (personId=$personId): ${e.message}")
            }
        }

        logger.info(
            "Portrait backfill complete — $successCount ok, $noEloCount no-elo, $failCount failed, " +
            "$tourneyInserts tournament inserts, $leagueUpdates league delta updates"
        )
    }

    /** Syncs a single player on demand (e.g. triggered from the player detail endpoint). */
    suspend fun syncPlayer(playerId: UUID, seasonId: UUID, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown) {
            val lastSync = syncCooldowns[playerId]
            if (lastSync != null && Instant.now().isBefore(lastSync.plus(COOLDOWN))) {
                logger.debug("Skipping sync for player $playerId — within cooldown window")
                return
            }
        }
        syncCooldowns[playerId] = Instant.now()
        syncPlayerDetailed(playerId, seasonId)
    }

    private data class SyncResult(
        val elo: Int?,
        val tournamentGamesInserted: Int,
        val leagueDeltasUpdated: Int
    )

    private suspend fun syncPlayerDetailed(playerId: UUID, seasonId: UUID): SyncResult {
        val personId = PlayerService.getClickTtIdById(playerId)
        if (personId == null) {
            logger.warn("Cannot sync player $playerId — no clickttId in database")
            return SyncResult(null, 0, 0)
        }

        logger.debug("Syncing personId=$personId")
        val portraitHtml = client.fetchPlayerPortrait(personId)

        val eloUrl = parser.extractEloProtokollUrl(portraitHtml)
            ?.cleanWosid()
            ?.takeIf { it.isNotBlank() && it != "#" }

        if (eloUrl == null) logger.debug("  personId=$personId — no Elo-Protokoll tab found")

        val eloHtml = if (eloUrl != null) {
            try {
                client.fetchUrl(eloUrl)
            } catch (e: Exception) {
                logger.warn("  personId=$personId — Elo-Protokoll fetch failed, ELO snapshot still saved: ${e.message}")
                null
            }
        } else null

        val portrait = parser.parsePlayerPortrait(portraitHtml, eloHtml, personId)
        logger.debug("  personId=$personId — elo=${portrait.currentElo}, games=${portrait.games.size}")

        if (portrait.currentElo != null) {
            PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
        } else {
            logger.debug("  personId=$personId — no current ELO on portrait page")
        }

        val (tourneyInserts, leagueUpdates) = if (portrait.games.isNotEmpty())
            processGames(playerId, portrait.games)
        else
            0 to 0

        return SyncResult(portrait.currentElo, tourneyInserts, leagueUpdates)
    }

    private suspend fun processGames(playerId: UUID, games: List<ClickTTGame>): Pair<Int, Int> {
        var tourneyInserts = 0
        var leagueUpdates  = 0

        for (game in games) {
            val opponentName = game.opponent.substringBefore("(").trim()
            val opponentId   = PlayerService.findPlayerIdByName(opponentName)
            val result       = if (game.isWin) GameResult.HOME else GameResult.AWAY

            val playedAt = LocalDate.parse(game.date, dateFormatter)
                .atStartOfDay(swissZone)
                .toOffsetDateTime()

            // First try to backfill the ELO delta on an existing league game row
            if (game.eloDelta != null) {
                val updated = GameService.updateLeagueGameEloDelta(playerId, opponentId, playedAt, game.eloDelta)
                if (updated) {
                    leagueUpdates++
                    continue
                }
            }

            // No league game found — insert as tournament game if not already stored
            if (!GameService.gameExists(playerId, playedAt, game.competition)) {
                GameService.insertTournamentGame(
                    playerId    = playerId,
                    opponentId  = opponentId,
                    playedAt    = playedAt,
                    competition = game.competition,
                    eloDelta    = game.eloDelta,
                    result      = result,
                    gameType    = GameType.SINGLES
                )
                tourneyInserts++
            }
        }

        return tourneyInserts to leagueUpdates
    }

    /**
     * Removes the WebObjects session ID from click-tt URLs.
     * The wosid parameter is session-specific and causes requests to fail when reused.
     */
    private fun String.cleanWosid(): String =
        replace(Regex("([?&])wosid=[^&]+&?"), "$1")
            .trimEnd('?', '&')
}
