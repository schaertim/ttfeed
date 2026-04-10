package com.ttfeed.service

import com.ttfeed.database.Federations
import com.ttfeed.model.FederationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

object FederationService {

    suspend fun getAll(): List<FederationResponse> {
        return withContext(Dispatchers.IO) {
            transaction {
                Federations.select(Federations.id, Federations.name)
                    .map { FederationResponse(
                        id   = it[Federations.id].toString(),
                        name = it[Federations.name]
                    )}
            }
        }
    }
}