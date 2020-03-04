package no.nav.rekrutteringsbistand.statistikk.application

import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.auth.AuthenticationConfig
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.security.token.support.test.FileResourceRetriever
import io.ktor.application.install
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.security.token.support.ktor.tokenValidationSupport

@KtorExperimentalAPI
fun lagApplicationEngine(
    env: Environment,
    database: DatabaseInterface
): ApplicationEngine {
    return embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter())
        }

        install(Authentication) {
            val config = AuthenticationConfig.tokenSupportConfig(env)
            if (env.isDev() || env.isProd()) {
                // TODO: Flytt denne ut
                tokenValidationSupport(config = config)
            } else {
                tokenValidationSupport(
                    config = config,
                    resourceRetriever = FileResourceRetriever("/local-login/metadata.json", "/local-login/jwkset.json")
                )
            }
        }

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                kandidatutfall()
                naisEndepunkt()
            }
        }
    }
}
