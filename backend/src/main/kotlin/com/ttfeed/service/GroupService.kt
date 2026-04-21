package com.ttfeed.service

import com.ttfeed.database.Federations
import com.ttfeed.database.Groups
import com.ttfeed.database.Seasons
import com.ttfeed.database.dbQuery
import com.ttfeed.model.GroupResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere

object GroupService {

    suspend fun getAll(league: String?, season: String?): List<GroupResponse> = dbQuery {
        var query = (Groups innerJoin Federations innerJoin Seasons)
            .select(
                Groups.id, Groups.name,
                Federations.name, Seasons.name,
                Groups.promotionSpots, Groups.relegationSpots
            )
        if (league != null) query = query.andWhere { Federations.name eq league }
        if (season != null) query = query.andWhere { Seasons.name eq season }
        query.map { it.toGroupResponse() }
    }

    suspend fun getById(groupId: String): GroupResponse? {
        val uuid = groupId.toUuidOrNull() ?: return null
        return dbQuery {
            (Groups innerJoin Federations innerJoin Seasons)
                .select(
                    Groups.id, Groups.name,
                    Federations.name, Seasons.name,
                    Groups.promotionSpots, Groups.relegationSpots
                )
                .where { Groups.id eq uuid }
                .firstOrNull()
                ?.toGroupResponse()
        }
    }

    private fun ResultRow.toGroupResponse() = GroupResponse(
        id              = this[Groups.id].toString(),
        name            = this[Groups.name],
        federation      = this[Federations.name],
        season          = this[Seasons.name],
        promotionSpots  = this[Groups.promotionSpots]?.toInt(),
        relegationSpots = this[Groups.relegationSpots]?.toInt(),
    )
}
