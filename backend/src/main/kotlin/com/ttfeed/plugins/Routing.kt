package com.ttfeed.plugins

import com.ttfeed.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            seasonRoutes()
            federationRoutes()
            divisionRoutes()
            groupRoutes()
            matchRoutes()
            playerRoutes()
        }
    }
}