package no.nav.rekrutteringsbistand.statistikk.nais

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
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
