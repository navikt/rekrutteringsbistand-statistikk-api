package no.nav.statistikkapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.Database
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerImpl
import no.nav.statistikkapi.kafka.KafkaConfig
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import no.nav.statistikkapi.kafka.hentUsendteUtfallOgSendPåKafka
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.kandidatutfall
import no.nav.statistikkapi.nais.naisEndepunkt
import no.nav.statistikkapi.stillinger.ElasticSearchKlientImpl
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

fun main() {
    val database = Database(Cluster.current)

    val tokenSupportConfig = TokenSupportConfig(
        IssuerConfig(
            name = "azuread",
            discoveryUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            acceptedAudience = listOf(System.getenv("AZURE_APP_CLIENT_ID")),
            cookieName = System.getenv("AZURE_OPENID_CONFIG_ISSUER")
        )
    )
    val tokenValidationConfig: AuthenticationConfig.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaConfig.producerConfig())

    val stillingssokProxyAccessTokenClient = AccessTokenProvider(
        config = AccessTokenProvider.Config(
            azureClientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
            azureClientId = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            scope = ElasticSearchKlientImpl.stillingssokProxyScope
        )
    )
    val elasticSearchKlient =
        ElasticSearchKlientImpl(tokenProvider = stillingssokProxyAccessTokenClient::getBearerToken)

    var ktor: Application? = null

    val rapid = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(System.getenv())
    ).withKtorModule {
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

            val stillingRepository = StillingRepository(database.dataSource)
            val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
            val stillingService = StillingService(elasticSearchKlient, stillingRepository)
            val sendKafkaMelding: Runnable =
                hentUsendteUtfallOgSendPåKafka(kandidatutfallRepository, datavarehusKafkaProducer, stillingService)
            val datavarehusScheduler = KafkaTilDataverehusScheduler(database.dataSource, sendKafkaMelding)

            routing {
                route("/rekrutteringsbistand-statistikk-api") {
                    naisEndepunkt(prometheusMeterRegistry)
                    kandidatutfall(kandidatutfallRepository, datavarehusScheduler)
                    hentStatistikk(kandidatutfallRepository)
                }
            }
            datavarehusScheduler.kjørPeriodisk()

        log.info("Applikasjon startet i miljø: ${Cluster.current}")

    }.build()

    Kandidathendelselytter(rapid)

    rapid.start()
}
