package no.nav.statistikkapi.nais

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.naisEndepunkt(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/isAlive") {
        call.respond("Alive")
    }

    get("/internal/isReady") {
        call.respond("Ready")
    }

    get("/internal/prometheus") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}
