package no.nav

import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val server = embeddedServer(
        Netty,
        // TODO: Ikke watching kjør i prod
        watchPaths = listOf("/"),
        port = 8080,
        module = Application::module
    )
    server.start(wait = true)
}

fun Application.module() {
    install(CallLogging)

    routing {
        route("/rekrutteringsbistand-statistikk-api") {
            naisEndepunkt()

            get("/") {
                call.respondText("{\"test\": 1337}", ContentType.Application.Json)
            }

        }
    }
}
