package com.ttfeed.jobs

import com.ttfeed.database.Federations
import com.ttfeed.database.Groups
import com.ttfeed.database.Seasons
import com.ttfeed.scraper.clicktt.ClickTTClient
import com.ttfeed.scraper.clicktt.ClickTTGroupScraper
import com.ttfeed.scraper.clicktt.ClickTTGroupScraper.GroupRef
import com.ttfeed.scraper.clicktt.ClickTTParser
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID

class NightlyGroupSyncJob(private val groupScraper: ClickTTGroupScraper) {
    private val logger = LoggerFactory.getLogger(NightlyGroupSyncJob::class.java)

    suspend fun run(seasonId: UUID, seasonName: String) {
        val groups = getGroupsForSeason(seasonId)
        logger.info("NightlyGroupSyncJob: refreshing ${groups.size} groups for season $seasonName")

        var refreshed = 0
        for (group in groups) {
            try {
                groupScraper.refreshGroupSchedule(group)
                groupScraper.refreshGroupStandings(group)
                refreshed++
            } catch (e: Exception) {
                logger.error("NightlyGroupSyncJob: failed group ${group.clickttId}: ${e.message}")
            }
            delay(500L)
        }

        logger.info("NightlyGroupSyncJob: complete — $refreshed / ${groups.size} groups refreshed")
    }

    private fun getGroupsForSeason(seasonId: UUID): List<GroupRef> = transaction {
        (Groups innerJoin Federations innerJoin Seasons)
            .select(Groups.id, Groups.clickttId, Federations.name, Seasons.name)
            .where {
                (Groups.seasonId eq seasonId) and
                Groups.clickttId.isNotNull()
            }
            .map {
                GroupRef(
                    dbId         = it[Groups.id],
                    clickttId    = it[Groups.clickttId]!!,
                    championship = ClickTTGroupScraper.toChampionship(it[Federations.name], it[Seasons.name])
                )
            }
    }

    companion object {
        fun create(): NightlyGroupSyncJob {
            val client = ClickTTClient()
            val parser = ClickTTParser()
            return NightlyGroupSyncJob(ClickTTGroupScraper(client, parser))
        }
    }
}
