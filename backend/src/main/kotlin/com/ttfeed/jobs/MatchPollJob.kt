package com.ttfeed.jobs

import com.ttfeed.database.Federations
import com.ttfeed.database.Groups
import com.ttfeed.database.Matches
import com.ttfeed.database.Seasons
import com.ttfeed.model.MatchStatus
import com.ttfeed.scraper.clicktt.ClickTTClient
import com.ttfeed.scraper.clicktt.ClickTTGroupScraper
import com.ttfeed.scraper.clicktt.ClickTTGroupScraper.GroupRef
import com.ttfeed.scraper.clicktt.ClickTTMatchScraper
import com.ttfeed.scraper.clicktt.ClickTTParser
import com.ttfeed.scraper.clicktt.ClickTTSyncService
import com.ttfeed.service.GameService
import com.ttfeed.service.SeasonService
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class MatchPollJob(
    private val groupScraper: ClickTTGroupScraper,
    private val matchScraper: ClickTTMatchScraper
) {
    private val logger = LoggerFactory.getLogger(MatchPollJob::class.java)

    suspend fun run() {
        val currentSeason = SeasonService.getCurrentSeason()
        if (currentSeason == null) {
            logger.warn("MatchPollJob: no season in database, skipping")
            return
        }
        val (seasonId, _) = currentSeason

        val groups = findGroupsWithPastDueMatches()
        if (groups.isEmpty()) {
            logger.debug("MatchPollJob: no groups with past-due scheduled matches")
            return
        }

        logger.info("MatchPollJob: refreshing ${groups.size} groups with past-due matches")

        val newlyCompletedMatchIds = mutableSetOf<java.util.UUID>()
        for (group in groups) {
            try {
                val completed = groupScraper.refreshGroupSchedule(group)
                newlyCompletedMatchIds.addAll(completed)
            } catch (e: Exception) {
                logger.error("MatchPollJob: failed to refresh group ${group.clickttId}: ${e.message}")
            }
        }

        if (newlyCompletedMatchIds.isEmpty()) {
            logger.debug("MatchPollJob: no newly completed matches found")
            return
        }

        logger.info("MatchPollJob: ${newlyCompletedMatchIds.size} matches completed — scraping game details")
        matchScraper.scrapeForMatches(newlyCompletedMatchIds)

        // Enqueue players from newly completed matches for ELO sync
        val playerIds = GameService.getPlayerIdsFromMatches(newlyCompletedMatchIds)
        logger.info("MatchPollJob: syncing ELO for ${playerIds.size} players from completed matches")
        for (playerId in playerIds) {
            try {
                ClickTTSyncService.syncPlayer(playerId, seasonId)
            } catch (e: Exception) {
                logger.warn("MatchPollJob: ELO sync failed for player $playerId: ${e.message}")
            }
            delay(500L)
        }

        logger.info("MatchPollJob: complete — ${newlyCompletedMatchIds.size} new matches, ${playerIds.size} players synced")
    }

    private fun findGroupsWithPastDueMatches(): List<GroupRef> = transaction {
        val now = OffsetDateTime.now()
        (Groups innerJoin Federations innerJoin Seasons innerJoin Matches)
            .select(Groups.id, Groups.clickttId, Federations.name, Seasons.name)
            .where {
                (Matches.status    eq MatchStatus.SCHEDULED) and
                (Matches.playedAt  less now) and
                (Groups.clickttId.isNotNull())
            }
            .withDistinct()
            .map {
                GroupRef(
                    dbId         = it[Groups.id],
                    clickttId    = it[Groups.clickttId]!!,
                    championship = ClickTTGroupScraper.toChampionship(it[Federations.name], it[Seasons.name])
                )
            }
    }

    companion object {
        fun create(): MatchPollJob {
            val client = ClickTTClient()
            val parser = ClickTTParser()
            return MatchPollJob(
                groupScraper = ClickTTGroupScraper(client, parser),
                matchScraper = ClickTTMatchScraper(client, parser)
            )
        }
    }
}
