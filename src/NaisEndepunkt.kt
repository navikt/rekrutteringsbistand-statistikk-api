package no.nav

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.naisEndepunkt() {
    get("/internal/isAlive") {
        call.respond("Alive")
    }
    get("/internal/isReady") {
        call.respond("Ready")
    }
}
