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
    private val logger = LoggerFactory.getLogger(ClickTTSyncService::class.java)
    private val client = ClickTTClient()
    private val parser = ClickTTParser()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val swissZone = ZoneId.of("Europe/Zurich")

    suspend fun runMonthlySync(seasonId: UUID) {
        val players = PlayerService.getAllLicensedPlayers()

        for ((playerId, licence) in players) {
            try {
                // ID direkt aus der Datenbank holen
                val personId = PlayerService.getClickTtIdById(playerId)

                if (personId != null) {
                    // 1. Portrait laden
                    val portraitHtml = client.fetchPlayerPortrait(personId)

                    // 2. Link zum Elo-Protokoll suchen und laden
                    val eloUrl = parser.extractEloProtokollUrl(portraitHtml)
                    val eloHtml = if (eloUrl != null) client.fetchUrl(eloUrl) else null

                    // 3. Beide Dokumente parsen
                    val portrait = parser.parsePlayerPortrait(portraitHtml, eloHtml, personId)

                    if (portrait.currentElo != null) {
                        PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
                    }

                    if (portrait.games.isNotEmpty()) {
                        processScrapedGames(playerId, portrait.games)
                    }
                } else {
                    logger.warn("Skipping player $playerId (Licence: $licence) - No clickttId in database.")
                }
            } catch (e: Exception) {
                logger.warn("Fetch failed for player $playerId: ${e.message}")
            }

            // Wichtig: Kurze Pause beim Massen-Sync
            delay(500L)
        }
    }

    suspend fun syncSinglePlayer(playerId: UUID, seasonId: UUID) {
        val personId = PlayerService.getClickTtIdById(playerId)

        if (personId == null) {
            logger.warn("Cannot sync player $playerId. No clickttId found in database.")
            return
        }

        try {
            logger.info("1. Fetching portrait HTML for clickttId: $personId")
            val portraitHtml = client.fetchPlayerPortrait(personId)

            // Hier holt er sich jetzt den echten /cgi-bin/.../eloFilter Link!
            val rawEloUrl = parser.extractEloProtokollUrl(portraitHtml)

            // Zur Sicherheit die WebObjects-Session aus dem String entfernen
            val eloUrl = rawEloUrl?.replace(Regex("([?&])wosid=[^&]+&?"), "$1")
                ?.removeSuffix("?")
                ?.removeSuffix("&")

            val eloHtml = if (eloUrl != null && eloUrl != "#") {
                logger.info("1b. Found Elo-Protokoll tab. Cleaned URL: $eloUrl")
                client.fetchUrl(eloUrl)
            } else {
                logger.warn("1b. No Elo-Protokoll tab found (or URL is dummy)! Games will be empty.")
                null
            }

            val portrait = parser.parsePlayerPortrait(portraitHtml, eloHtml, personId)
            logger.info("2. Parsed profile. currentElo=${portrait.currentElo}, found ${portrait.games.size} games.")

            if (portrait.currentElo != null) {
                PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
            }

            if (portrait.games.isNotEmpty()) {
                processScrapedGames(playerId, portrait.games)
            }
        } catch (e: Exception) {
            logger.error("Single fetch failed for personId $personId", e)
            throw e
        }
    }

    private suspend fun processScrapedGames(playerId: UUID, games: List<ClickTTGame>) {
        for (game in games) {
            val cleanOpponentName = game.opponent.substringBefore("(").trim()
            val opponentId = PlayerService.findPlayerIdByName(cleanOpponentName)
            val result = if (game.isWin) GameResult.HOME else GameResult.AWAY

            val playedAt = LocalDate.parse(game.date, dateFormatter)
                .atStartOfDay(swissZone)
                .toOffsetDateTime()

            val exists = GameService.gameExists(playerId, playedAt, game.competition)

            if (!exists) {
                GameService.insertTournamentGame(
                    playerId = playerId,
                    opponentId = opponentId,
                    playedAt = playedAt,
                    competition = game.competition,
                    eloDelta = game.eloDelta,
                    result = result,
                    gameType = GameType.SINGLES
                )
            }
        }
    }
}