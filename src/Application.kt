package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever

fun main() {
    val profil: String = System.getenv("PROFIL") ?: "lokal"
    val server = embeddedServer(
        Netty,
        watchPaths = if (profil == "lokal") listOf("/no/nav/rekrutteringsbistand/statistikk") else emptyList(),
        port = 8080,
        module = Application::module
    )
    server.start(wait = true)
}

@KtorExperimentalAPI
fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    install(Authentication) {
        // TODO: miljø configs

        // lokal
        val issuerConfig = IssuerConfig(
            name = "isso",
            discoveryUrl = "http://metadata",
            acceptedAudience = listOf("aud-localhost", "aud-isso"),
            cookieName = "isso-idtoken"
        )

        tokenValidationSupport(
            config = TokenSupportConfig(issuerConfig),
            resourceRetriever = FileResourceRetriever("/local-login/metadata.json", "/local-login/jwkset.json")
        )
    }

    val environment = Environment()
    val database: DatabaseInterface = if (environment.profil == "lokal") {
        TestDatabase()
    } else {
        Database(environment)
    }

    routing {
        route("/rekrutteringsbistand-statistikk-api") {
            naisEndepunkt()
            kandidatutfall()

            authenticate {
                get("/closed") {
                    call.respondText("{\"test\": 1337}", ContentType.Application.Json)
                }
            }
            get("/open") {
                call.respondText("{\"test\": 1337}", ContentType.Application.Json)
            }
        }
    }
}
