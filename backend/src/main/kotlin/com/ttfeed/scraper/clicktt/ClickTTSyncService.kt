package com.ttfeed.scraper.clicktt

import com.ttfeed.scraper.clicktt.model.ClickTTGame
import com.ttfeed.scraper.knob.GameResult
import com.ttfeed.service.GameService
import com.ttfeed.service.PlayerService
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
                val searchHtml = client.searchPlayerByLicence(licence)
                val personId = parser.extractPersonIdFromSearch(searchHtml)

                if (personId != null) {
                    val portraitHtml = client.fetchPlayerPortrait(personId)
                    val portrait = parser.parsePlayerPortrait(portraitHtml, personId)

                    if (portrait.currentElo != null) {
                        PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
                    }

                    if (portrait.games.isNotEmpty()) {
                        processScrapedGames(playerId, portrait.games)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Fetch failed for licence $licence: ${e.message}")
            }
        }
    }

    suspend fun syncSinglePlayer(playerId: UUID, seasonId: UUID) {
        val licence = PlayerService.getLicenceNrById(playerId) ?: return

        if (licence.startsWith("knob:")) return

        try {
            val searchHtml = client.searchPlayerByLicence(licence)
            val personId = parser.extractPersonIdFromSearch(searchHtml)

            if (personId != null) {
                val portraitHtml = client.fetchPlayerPortrait(personId)
                val portrait = parser.parsePlayerPortrait(portraitHtml, personId)

                if (portrait.currentElo != null) {
                    PlayerService.saveBaseElo(playerId, seasonId, portrait.currentElo)
                }

                if (portrait.games.isNotEmpty()) {
                    processScrapedGames(playerId, portrait.games)
                }
            }
        } catch (e: Exception) {
            logger.warn("Single fetch failed for licence $licence: ${e.message}")
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
                    result = result
                )
            }
        }
    }
}