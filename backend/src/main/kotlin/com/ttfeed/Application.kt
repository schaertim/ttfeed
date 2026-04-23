package com.ttfeed

import com.ttfeed.database.configureDatabase
import com.ttfeed.jobs.MatchPollJob
import com.ttfeed.jobs.NightlyGroupSyncJob
import com.ttfeed.plugins.configureCors
import com.ttfeed.plugins.configureRouting
import com.ttfeed.plugins.configureSerialization
import com.ttfeed.scraper.clicktt.ClickTTSeasonScraper
import com.ttfeed.scraper.clicktt.ClickTTSyncService
import com.ttfeed.scraper.knob.BackfillScraper
import com.ttfeed.service.SeasonService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    configureCors()

    // Portrait backfill
    launch(Dispatchers.IO) {
        val currentSeason = SeasonService.getCurrentSeason()
        if (currentSeason != null) {
            ClickTTSyncService.runPortraitBackfill(currentSeason.first)
        }
    }

    // Match poll — every 10 minutes, checks groups with past-due scheduled matches
    launch(Dispatchers.IO) {
        while (isActive) {
            delay(10.minutes)
            try {
                MatchPollJob.create().run()
            } catch (e: Exception) {
                environment.log.error("MatchPollJob failed", e)
            }
        }
    }

    // Nightly group sync — runs once per night at 03:00 Swiss time
    launch(Dispatchers.IO) {
        while (isActive) {
            delay(millisUntil(hour = 3, zone = ZoneId.of("Europe/Zurich")))
            try {
                val season = SeasonService.getCurrentSeason() ?: continue
                NightlyGroupSyncJob.create().run(season.first, season.second)
            } catch (e: Exception) {
                environment.log.error("NightlyGroupSyncJob failed", e)
            }
        }
    }
}

/** Returns the milliseconds until the next occurrence of [hour]:00 in [zone]. */
private fun millisUntil(hour: Int, zone: ZoneId): Long {
    val now  = ZonedDateTime.now(zone)
    var next = now.toLocalDate().atStartOfDay(zone).withHour(hour)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
}