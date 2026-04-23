package com.ttfeed.service

import com.ttfeed.database.Seasons
import com.ttfeed.database.dbQuery
import com.ttfeed.model.SeasonResponse
import org.jetbrains.exposed.sql.SortOrder
import java.util.*

object SeasonService {

    suspend fun getAll(): List<SeasonResponse> = dbQuery {
        Seasons.select(Seasons.id, Seasons.name)
            .orderBy(Seasons.name to SortOrder.DESC)
            .map { SeasonResponse(id = it[Seasons.id].toString(), name = it[Seasons.name]) }
    }

    suspend fun getCurrentSeasonId(): UUID? = dbQuery {
        Seasons.select(Seasons.id)
            .orderBy(Seasons.name to SortOrder.DESC)
            .limit(1)
            .map { it[Seasons.id] }
            .firstOrNull()
    }

    suspend fun getCurrentSeason(): Pair<UUID, String>? = dbQuery {
        Seasons.select(Seasons.id, Seasons.name)
            .orderBy(Seasons.name to SortOrder.DESC)
            .limit(1)
            .map { it[Seasons.id] to it[Seasons.name] }
            .firstOrNull()
    }
}
