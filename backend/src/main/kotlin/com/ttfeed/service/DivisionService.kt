package com.ttfeed.service

import com.ttfeed.database.Divisions
import com.ttfeed.database.Federations
import com.ttfeed.database.Groups
import com.ttfeed.database.Seasons
import com.ttfeed.database.dbQuery
import com.ttfeed.model.DivisionResponse
import com.ttfeed.model.GroupResponse
import com.ttfeed.util.toUuidOrNull
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere

object DivisionService {

    suspend fun getAll(league: String?, season: String?): List<DivisionResponse> = dbQuery {
        var query = (Divisions innerJoin Federations innerJoin Seasons)
            .select(Divisions.id, Divisions.name, Federations.name, Seasons.name)

        if (league != null) query = query.andWhere { Federations.name eq league }
        if (season != null) query = query.andWhere { Seasons.name eq season }

        query.map { it.toDivisionResponse() }
    }

    suspend fun getGroups(divisionId: String): List<GroupResponse>? {
        val uuid = divisionId.toUuidOrNull() ?: return null
        return dbQuery {
            Groups
                .select(Groups.id, Groups.name, Groups.promotionSpots, Groups.relegationSpots)
                .where { Groups.divisionId eq uuid }
                .map { it.toGroupResponse() }
        }
    }

    private fun ResultRow.toDivisionResponse() = DivisionResponse(
        id         = this[Divisions.id].toString(),
        name       = this[Divisions.name],
        federation = this[Federations.name],
        season     = this[Seasons.name],
    )

    private fun ResultRow.toGroupResponse() = GroupResponse(
        id              = this[Groups.id].toString(),
        name            = this[Groups.name],
        promotionSpots  = this[Groups.promotionSpots]?.toInt(),
        relegationSpots = this[Groups.relegationSpots]?.toInt(),
    )
}
