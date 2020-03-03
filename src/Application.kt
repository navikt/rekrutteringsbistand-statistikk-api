package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.auth.AuthenticationConfig
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.rekrutteringsbistand.statistikk.utils.Log
import no.nav.rekrutteringsbistand.statistikk.utils.clusterEnvVar
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever

@KtorExperimentalAPI
fun main() {
    val skalKjøreAutoreload = System.getenv(clusterEnvVar) == null
    val server = embeddedServer(
        Netty,
        watchPaths = if (skalKjøreAutoreload) listOf("/no/nav/rekrutteringsbistand/statistikk") else emptyList(),
        port = 8080,
        module = Application::module
    )
    server.start(wait = true)
}

@KtorExperimentalAPI
fun Application.module() {
    val env = Environment()
    Log.info("Kjører i miljø: ${env.miljø}")

    install(CallLogging)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    install(Authentication) {
        val config = AuthenticationConfig.tokenSupportConfig(env)
        if (env.isDev() || env.isProd()) {
            tokenValidationSupport(config = config)
        } else {
            tokenValidationSupport(
                config = config,
                resourceRetriever = FileResourceRetriever("/local-login/metadata.json", "/local-login/jwkset.json")
            )
        }
    }

    if (env.isDev() || env.isProd()) {
        Database(env)
    } else {
        TestDatabase()
    }

    routing {
        route("/rekrutteringsbistand-statistikk-api") {
            kandidatutfall()
            naisEndepunkt()
        }
    }
}
