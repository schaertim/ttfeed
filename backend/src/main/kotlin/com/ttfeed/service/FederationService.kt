package com.ttfeed.service

import com.ttfeed.database.Federations
import com.ttfeed.database.dbQuery
import com.ttfeed.model.FederationResponse

object FederationService {

    suspend fun getAll(): List<FederationResponse> = dbQuery {
        Federations.select(Federations.id, Federations.name)
            .map { FederationResponse(id = it[Federations.id].toString(), name = it[Federations.name]) }
    }
}
