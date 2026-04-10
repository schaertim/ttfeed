package com.ttfeed.routes

import com.ttfeed.service.FederationService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.federationRoutes() {
    route("/federations") {
        get {
            call.respond(FederationService.getAll())
        }
    }
}