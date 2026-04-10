package com.ttfeed.routes

import com.ttfeed.service.DivisionService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.divisionRoutes() {
    route("/divisions") {
        get {
            val league = call.request.queryParameters["league"]
            val season = call.request.queryParameters["season"]
            val divisions = DivisionService.getAll(league, season)
            call.respond(divisions)
        }

        get("/{id}/groups") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing division id")
            val groups = DivisionService.getGroups(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Division not found")
            call.respond(groups)
        }

        get("/{id}") {}
    }
}