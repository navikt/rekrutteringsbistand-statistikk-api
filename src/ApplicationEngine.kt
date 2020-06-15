package no.nav.rekrutteringsbistand.statistikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt

@KtorExperimentalAPI
fun lagApplicationEngine(
    port: Int = 8080,
    database: DatabaseInterface,
    tokenValidationConfig: Authentication.Configuration.() -> Unit
): ApplicationEngine {
    return embeddedServer(Netty, port) {
        install(CallLogging)
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(Authentication, tokenValidationConfig)

        val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
        }
        Metrics.addRegistry(prometheusMeterRegistry)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                naisEndepunkt(prometheusMeterRegistry)
                kandidatutfall(database)
            }
        }
    }
}
