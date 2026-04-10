package com.ttfeed.service

import com.ttfeed.database.Seasons
import com.ttfeed.model.SeasonResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

object SeasonService {

    suspend fun getAll(): List<SeasonResponse> {
        return withContext(Dispatchers.IO) {
            transaction {
                Seasons.select(Seasons.id, Seasons.name)
                    .orderBy(Seasons.name to SortOrder.DESC)
                    .map { SeasonResponse(
                        id   = it[Seasons.id].toString(),
                        name = it[Seasons.name]
                    )}
            }
        }
    }
}