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
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaTilDataverehusScheduler
import no.nav.rekrutteringsbistand.statistikk.kafka.sendKafkaMeldingTilDatavarehus
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt
import javax.sql.DataSource

@KtorExperimentalAPI
fun lagApplicationEngine(
    port: Int = 8111,
    dataSource: DataSource,
    tokenValidationConfig: Authentication.Configuration.() -> Unit,
    datavarehusKafkaProducer: DatavarehusKafkaProducer
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

        val repository = Repository(dataSource)
        val sendKafkaMelding: Runnable = sendKafkaMeldingTilDatavarehus(repository, datavarehusKafkaProducer)
        val scheduler = KafkaTilDataverehusScheduler(dataSource, sendKafkaMelding);

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                naisEndepunkt(prometheusMeterRegistry)
                kandidatutfall(repository, scheduler)
            }
        }

        scheduler.executePeriodically()
    }
}

