package no.nav.statistikkapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport
import no.nav.statistikkapi.db.Database
import no.nav.statistikkapi.kafka.*
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatliste.KandidatlistehendelseLytter
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.metrikker.MetrikkJobb
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoLytter
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId.of
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.MILLIS
import javax.sql.DataSource

fun main() {
    try {
        val tokenSupportConfig = TokenSupportConfig(
            IssuerConfig(
                name = "azuread",
                discoveryUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
                acceptedAudience = listOf(System.getenv("AZURE_APP_CLIENT_ID")),
            )
        )
        val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaProducer(KafkaConfig.producerConfig()))
        startApp(Database(Cluster.current), tokenSupportConfig, datavarehusKafkaProducer)
    } catch (e: Exception) {
        val log = LoggerFactory.getLogger("no.nav.statistikkapi.applicationKt")
        log.error("Feil i applikasjon", e)
        throw e
    }
}

fun startApp(
    database: Database,
    tokenSupportConfig: TokenSupportConfig,
    datavarehusKafkaProducer: DatavarehusKafkaProducer
) {
    val tokenValidationConfig: AuthenticationConfig.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    startDatavarehusScheduler(database, datavarehusKafkaProducer)

    val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
    val kandidatlisteRepository = KandidatlisteRepository(database.dataSource)
    val visningKontaktinfoRepository = VisningKontaktinfoRepository(database.dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val metrikkJobb = MetrikkJobb(
        kandidatutfallRepository,
        kandidatlisteRepository,
        visningKontaktinfoRepository,
        prometheusMeterRegistry
    )

    val rapid = RapidApplication.create(
        System.getenv(),
        builder = {
            withKtorModule {
                settOppKtor(
                    application = this,
                    tokenValidationConfig = tokenValidationConfig,
                    dataSource = database.dataSource,
                    prometheusMeterRegistry = prometheusMeterRegistry
                )
            }
                .build().apply {
                    PresenterteOgFåttJobbenKandidaterLytter(
                        this,
                        LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        ),
                        "RegistrertDeltCv",
                        prometheusMeterRegistry = prometheusMeterRegistry
                    )
                    PresenterteOgFåttJobbenKandidaterLytter(
                        this,
                        LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        ),
                        "RegistrertFåttJobben",
                        prometheusMeterRegistry = prometheusMeterRegistry
                    )
                    ReverserPresenterteOgFåttJobbenKandidaterLytter(
                        this,
                        LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        ),
                        utfallRepository = kandidatutfallRepository,
                        "FjernetRegistreringDeltCv",
                        prometheusMeterRegistry = prometheusMeterRegistry
                    )
                    ReverserPresenterteOgFåttJobbenKandidaterLytter(
                        this,
                        LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        ),
                        utfallRepository = kandidatutfallRepository,
                        "FjernetRegistreringFåttJobben",
                        prometheusMeterRegistry = prometheusMeterRegistry
                    )
                    SendtTilArbeidsgiverKandidaterLytter(
                        this,
                        LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        ),
                        prometheusMeterRegistry = prometheusMeterRegistry
                    )
                    SlettetStillingOgKandidatlisteLytter(
                        rapidsConnection = this,
                        repository = KandidatutfallRepository(database.dataSource),
                        prometheusMeterRegistry = prometheusMeterRegistry,
                        lagreUtfallOgStilling = LagreUtfallOgStilling(
                            KandidatutfallRepository(database.dataSource),
                            StillingRepository(database.dataSource)
                        )
                    )
                    KandidatlistehendelseLytter(
                        rapidsConnection = this,
                        repository = KandidatlisteRepository(database.dataSource)
                    )
                    VisningKontaktinfoLytter(
                        rapidsConnection = this,
                        repository = visningKontaktinfoRepository
                    )

                }
        }
    )

    metrikkJobb.start()
    rapid.start()
}

private fun startDatavarehusScheduler(
    database: Database,
    datavarehusKafkaProducer: DatavarehusKafkaProducer
) {
    val stillingRepository = StillingRepository(database.dataSource)
    val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
    val sendKafkaMelding: Runnable =
        hentUsendteUtfallOgSendPåKafka(kandidatutfallRepository, datavarehusKafkaProducer, stillingRepository)
    val datavarehusScheduler = KafkaTilDataverehusScheduler(database.dataSource, sendKafkaMelding)

    datavarehusScheduler.kjørPeriodisk()
}

fun defaultProperties(objectMapper: ObjectMapper) = objectMapper.apply {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable((DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
}

fun settOppKtor(
    application: Application,
    tokenValidationConfig: AuthenticationConfig.() -> Unit,
    dataSource: DataSource,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    application.apply {
        install(CallLogging) {
            level = Level.DEBUG
            filter { call ->
                !setOf(
                    "isalive",
                    "isready",
                    "metrics"
                ).contains(call.request.document())
            }
            disableDefaultColors()
        }
        install(ContentNegotiation) {
            jackson {
                defaultProperties(this)
            }
        }
        install(Authentication, tokenValidationConfig)

        Metrics.addRegistry(prometheusMeterRegistry)

        val kandidatutfallRepository = KandidatutfallRepository(dataSource)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                hentStatistikk(kandidatutfallRepository)
                get("/metrics") {
                    call.respond(prometheusMeterRegistry.scrape())
                }
            }
        }

        log.info("Ktor satt opp i miljø: ${Cluster.current}")

    }
}

/**
 * Tidspunkt uten nanosekunder, for å unngå at to like tidspunkter blir ulike pga at database og Microsoft Windws håndterer nanos annerledes enn Mac og Linux.
 */
fun nowOslo(): ZonedDateTime = ZonedDateTime.now().atOslo()

fun ZonedDateTime.atOslo(): ZonedDateTime = this.withZoneSameInstant(of("Europe/Oslo")).truncatedTo(MILLIS)

fun LocalDateTime.atOslo(): ZonedDateTime = this.atZone(of("Europe/Oslo")).truncatedTo(MILLIS)

fun Instant.atOslo(): ZonedDateTime = this.atZone(of("Europe/Oslo")).truncatedTo(MILLIS)
