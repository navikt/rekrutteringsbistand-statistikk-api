package no.nav

import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging)

    routing {
        route("/rekrutteringsbistand-statistikk-api") {
            naisEndepunkt()

            get("/") {
                call.respondText("Hello World!")
            }
        }
    }
}

