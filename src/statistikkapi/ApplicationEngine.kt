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
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import statistikkapi.kafka.DatavarehusKafkaProducer
import statistikkapi.kafka.KafkaTilDataverehusScheduler
import statistikkapi.kafka.hentUsendteUtfallOgSendPåKafka
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.kandidatutfall.kandidatutfall
import statistikkapi.nais.naisEndepunkt
import statistikkapi.stillinger.ElasticSearchKlient
import statistikkapi.stillinger.StillingRepository
import statistikkapi.stillinger.StillingService
import javax.sql.DataSource

fun lagApplicationEngine(
    port: Int = 8111,
    dataSource: DataSource,
    tokenValidationConfig: Authentication.Configuration.() -> Unit,
    datavarehusKafkaProducer: DatavarehusKafkaProducer,
    elasticSearchKlient: ElasticSearchKlient
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

        val kandidatutfallRepository = KandidatutfallRepository(dataSource)
        val sendKafkaMelding: Runnable = hentUsendteUtfallOgSendPåKafka(kandidatutfallRepository, datavarehusKafkaProducer)

        val datavarehusScheduler = KafkaTilDataverehusScheduler(dataSource, sendKafkaMelding)

        val stillingRepository = StillingRepository(dataSource)
        val stillingService = StillingService(elasticSearchKlient, stillingRepository)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                naisEndepunkt(prometheusMeterRegistry)
                kandidatutfall(kandidatutfallRepository, datavarehusScheduler, stillingService)
                hentStatistikk(kandidatutfallRepository)
            }
        }

        datavarehusScheduler.kjørPeriodisk()
    }
}
