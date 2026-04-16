package com.ttfeed.scraper.clicktt

import com.ttfeed.model.GameResult
import com.ttfeed.model.GameType
import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.service.GameService
import com.ttfeed.service.PlayerService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object ClickTTSyncService {
    private val logger        = LoggerFactory.getLogger(ClickTTSyncService::class.java)
    private val client        = ClickTTClient()
    private val parser        = ClickTTParser()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val swissZone     = ZoneId.of("Europe/Zurich")

    /** Syncs ELO and game history for all licensed players. Run once per month. */
    suspend fun runMonthlySync(seasonId: UUID) {
        val players = PlayerService.getAllLicensedPlayers()
        logger.info("Monthly sync starting — ${players.size} licensed players")

        for ((playerId, licence) in players) {
            try {
                syncPlayer(playerId, seasonId)
            } catch (e: Exception) {
                logger.warn("Sync failed for player $playerId (licence=$licence): ${e.message}")
            }
            // Throttle bulk syncs to avoid hammering click-tt
            delay(500L)
        }

        logger.info("Monthly sync complete")
    }

    /** Syncs a single player on demand (e.g. triggered from the player detail endpoint). */
    suspend fun syncPlayer(playerId: UUID, seasonId: UUID) {
        val personId = PlayerService.getClickTtIdById(playerId)
        if (personId == null) {
            logger.warn("Cannot sync player $playerId — no clickttId in database")
            return
        }

        val portraitHtml = client.fetchPlayerPortrait(personId)

        val eloUrl = parser.extractEloProtokollUrl(portraitHtml)
            ?.cleanWosid()
            ?.takeIf { it.isNotBlank() && it != "#" }

        val eloHtml = if (eloUrl != null) {
            client.fetchUrl(eloUrl)
        } else {
            logger.debug("No Elo-Protokoll tab found for personId=$personId — games will be empty")
            null
        }

        val portrait = parser.parsePlayerPortrait(portraitHtml, eloHtml, personId)
        logger.debug("Parsed profile for personId=$personId — elo=${portrait.currentElo}, games=${portrait.games.size}")

        if (portrait.currentElo != null) {
            PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
        }

        if (portrait.games.isNotEmpty()) {
            processGames(playerId, portrait.games)
        }
    }

    private suspend fun processGames(playerId: UUID, games: List<ClickTTGame>) {
        for (game in games) {
            val opponentName = game.opponent.substringBefore("(").trim()
            val opponentId   = PlayerService.findPlayerIdByName(opponentName)
            val result       = if (game.isWin) GameResult.HOME else GameResult.AWAY

            val playedAt = LocalDate.parse(game.date, dateFormatter)
                .atStartOfDay(swissZone)
                .toOffsetDateTime()

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
            }
        }
    }

    /**
     * Removes the WebObjects session ID from click-tt URLs.
     * The wosid parameter is session-specific and causes requests to fail when reused.
     */
    private fun String.cleanWosid(): String =
        replace(Regex("([?&])wosid=[^&]+&?"), "$1")
            .trimEnd('?', '&')
}
