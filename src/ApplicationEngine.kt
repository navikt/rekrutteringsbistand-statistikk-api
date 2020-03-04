package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt

@KtorExperimentalAPI
fun lagApplicationEngine(
    database: DatabaseInterface,
    tokenValidationConfig: Authentication.Configuration.() -> Unit
): ApplicationEngine {
    return embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter())
        }
        install(Authentication, tokenValidationConfig)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                kandidatutfall()
                naisEndepunkt()
            }
        }
    }
}
