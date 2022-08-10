package no.nav.statistikkapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import no.nav.statistikkapi.kafka.hentUsendteUtfallOgSendPåKafka
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.kandidatutfall
import no.nav.statistikkapi.nais.naisEndepunkt
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingService
import javax.sql.DataSource

fun settOppKtor(
    application: Application,
    tokenValidationConfig: AuthenticationConfig.() -> Unit,
    dataSource: DataSource,
    elasticSearchKlient: ElasticSearchKlient,
    datavarehusKafkaProducer: DatavarehusKafkaProducer
) {
    application.apply {
        install(CallLogging)
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(Authentication, tokenValidationConfig)

        val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        //install(MicrometerMetrics) { registry = prometheusMeterRegistry }
        Metrics.addRegistry(prometheusMeterRegistry)

        val stillingRepository = StillingRepository(dataSource)
        val kandidatutfallRepository = KandidatutfallRepository(dataSource)
        val stillingService = StillingService(elasticSearchKlient, stillingRepository)
        val sendKafkaMelding: Runnable =
            hentUsendteUtfallOgSendPåKafka(kandidatutfallRepository, datavarehusKafkaProducer, stillingService)
        val datavarehusScheduler = KafkaTilDataverehusScheduler(dataSource, sendKafkaMelding)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                naisEndepunkt(prometheusMeterRegistry)
                kandidatutfall(kandidatutfallRepository, datavarehusScheduler)
                hentStatistikk(kandidatutfallRepository)
            }
        }
        datavarehusScheduler.kjørPeriodisk()

        log.info("Ktor satt opp i miljø: ${Cluster.current}")
    }
}



