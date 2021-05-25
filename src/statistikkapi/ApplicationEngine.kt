package statistikkapi

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
import statistikkapi.datakatalog.DatakatalogKlient
import statistikkapi.datakatalog.DatakatalogScheduler
import statistikkapi.datakatalog.DatakatalogStatistikk
import statistikkapi.datakatalog.DatakatalogUrl
import statistikkapi.kafka.DatavarehusKafkaProducer
import statistikkapi.kafka.KafkaTilDataverehusScheduler
import statistikkapi.kafka.hentUsendteUtfallOgSendPåKafka
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.kandidatutfall.kandidatutfall
import statistikkapi.nais.naisEndepunkt
import java.time.LocalDate
import javax.sql.DataSource

@KtorExperimentalAPI
fun lagApplicationEngine(
    port: Int = 8111,
    dataSource: DataSource,
    tokenValidationConfig: Authentication.Configuration.() -> Unit,
    datavarehusKafkaProducer: DatavarehusKafkaProducer,
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

        val repository = KandidatutfallRepository(dataSource)
        val sendKafkaMelding: Runnable = hentUsendteUtfallOgSendPåKafka(repository, datavarehusKafkaProducer)
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

