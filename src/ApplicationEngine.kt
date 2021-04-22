package no.nav.rekrutteringsbistand.statistikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.finn.unleash.Unleash
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogKlient
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogUrl
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogScheduler
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaTilDataverehusScheduler
import no.nav.rekrutteringsbistand.statistikk.kafka.hentUsendteUtfallOgSendPåKafka
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.nais.naisEndepunkt
import java.time.LocalDate
import javax.sql.DataSource

@KtorExperimentalAPI
fun lagApplicationEngine(
    port: Int = 8111,
    dataSource: DataSource,
    tokenValidationConfig: Authentication.Configuration.() -> Unit,
    datavarehusKafkaProducer: DatavarehusKafkaProducer,
    unleash: Unleash,
    url: DatakatalogUrl
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
        val sendKafkaMelding: Runnable = hentUsendteUtfallOgSendPåKafka(repository, datavarehusKafkaProducer, unleash)
        val datavarehusScheduler = KafkaTilDataverehusScheduler(dataSource, sendKafkaMelding)

        val sendHullICvTilDatakatalog = DatakatalogStatistikk(repository, DatakatalogKlient(url = url), dagensDato = { LocalDate.now() })
        val hullICvTilDatakatalogScheduler = DatakatalogScheduler(dataSource, sendHullICvTilDatakatalog)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                naisEndepunkt(prometheusMeterRegistry)
                kandidatutfall(repository, datavarehusScheduler)
                hentStatistikk(repository)
            }
        }

        datavarehusScheduler.kjørPeriodisk()
        hullICvTilDatakatalogScheduler.kjørPeriodisk()
    }
}

