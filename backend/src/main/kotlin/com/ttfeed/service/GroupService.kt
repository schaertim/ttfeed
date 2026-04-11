package com.ttfeed.service

import com.ttfeed.database.Groups
import com.ttfeed.model.GroupResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object GroupService {

    suspend fun getById(groupId: String): GroupResponse? {
        val uuid = runCatching { UUID.fromString(groupId) }.getOrNull() ?: return null

        return withContext(Dispatchers.IO) {
            transaction {
                Groups
                    .select(Groups.id, Groups.name, Groups.promotionSpots, Groups.relegationSpots)
                    .where { Groups.id eq uuid }
                    .firstOrNull()
                    ?.toGroupResponse()
            }
        }
    }

    private fun ResultRow.toGroupResponse() = GroupResponse(
        id              = this[Groups.id].toString(),
        name            = this[Groups.name],
        promotionSpots  = this[Groups.promotionSpots]?.toInt(),
        relegationSpots = this[Groups.relegationSpots]?.toInt(),
    )
}