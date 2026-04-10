package com.ttfeed.service

import com.ttfeed.database.Players
import com.ttfeed.model.PagedResponse
import com.ttfeed.model.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.LikePattern
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PlayerService {

    suspend fun getById(playerId: String): PlayerResponse? {
        val uuid = runCatching { UUID.fromString(playerId) }.getOrNull() ?: return null

        return withContext(Dispatchers.IO) {
            transaction {
                Players.select(Players.id, Players.fullName, Players.licenceNr)
                    .where { Players.id eq uuid }
                    .firstOrNull()
                    ?.let { PlayerResponse(
                        id        = it[Players.id].toString(),
                        fullName  = it[Players.fullName],
                        licenceNr = it[Players.licenceNr]
                    )}
            }
        }
    }

    suspend fun search(name: String, page: Int, size: Int): PagedResponse<PlayerResponse>? {
        if (name.length < 3) return null

        return withContext(Dispatchers.IO) {
            transaction {
                val pattern = LikePattern("%${name.lowercase()}%")

                val total = Players.select(Players.id)
                    .where { Players.fullName like pattern }
                    .count()

                val items = Players.select(Players.id, Players.fullName, Players.licenceNr)
                    .where { Players.fullName like pattern }
                    .orderBy(Players.fullName to SortOrder.ASC)
                    .limit(size).offset(start = (page * size).toLong())
                    .map { PlayerResponse(
                        id        = it[Players.id].toString(),
                        fullName  = it[Players.fullName],
                        licenceNr = it[Players.licenceNr]
                    )}

                PagedResponse(
                    items = items,
                    page  = page,
                    size  = size,
                    total = total
                )
            }
        }
    }
}