package statistikkapi.nais

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
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
