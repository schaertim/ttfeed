package com.ttfeed.service

import com.ttfeed.database.Groups
import com.ttfeed.database.dbQuery
import com.ttfeed.model.GroupResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.ResultRow

object GroupService {

    suspend fun getById(groupId: String): GroupResponse? {
        val uuid = groupId.toUuidOrNull() ?: return null
        return dbQuery {
            Groups
                .select(Groups.id, Groups.name, Groups.promotionSpots, Groups.relegationSpots)
                .where { Groups.id eq uuid }
                .firstOrNull()
                ?.toGroupResponse()
        }
    }

    private fun ResultRow.toGroupResponse() = GroupResponse(
        id              = this[Groups.id].toString(),
        name            = this[Groups.name],
        promotionSpots  = this[Groups.promotionSpots]?.toInt(),
        relegationSpots = this[Groups.relegationSpots]?.toInt(),
    )
}
