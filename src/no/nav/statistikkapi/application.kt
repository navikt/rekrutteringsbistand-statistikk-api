package no.nav.statistikkapi


import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.Database
import no.nav.statistikkapi.kafka.*
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.statistikkjobb.Statistikkjobb
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.tiltak.TiltakManglerAktørIdLytter
import no.nav.statistikkapi.tiltak.Tiltaklytter
import no.nav.statistikkapi.tiltak.TiltaksRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId.of
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.MILLIS
import javax.sql.DataSource

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

fun main() {
    val tokenSupportConfig = TokenSupportConfig(
        IssuerConfig(
            name = "azuread",
            discoveryUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            acceptedAudience = listOf(System.getenv("AZURE_APP_CLIENT_ID")),
            cookieName = System.getenv("AZURE_OPENID_CONFIG_ISSUER")
        )
    )
    val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaProducer(KafkaConfig.producerConfig()))
    startApp(Database(Cluster.current), tokenSupportConfig, datavarehusKafkaProducer)
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
    val stillingRepository = StillingRepository(database.dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val statistikkjobb = Statistikkjobb(kandidatutfallRepository, prometheusMeterRegistry)

    val rapid = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(
            System.getenv()
        )
    ).withKtorModule {
        settOppKtor(
            application = this,
            tokenValidationConfig = tokenValidationConfig,
            dataSource = database.dataSource,
            prometheusMeterRegistry = prometheusMeterRegistry
        )
    }.build().apply {
        Kandidathendelselytter(
            rapidsConnection = this,
            repo = kandidatutfallRepository,
            stillingRepository = stillingRepository,
            prometheusMeterRegistry = prometheusMeterRegistry
        )

        PresenterteOgFåttJobbenKandidaterLytter(
            this,
            LagreUtfallOgStilling(
                KandidatutfallRepository(database.dataSource),
                StillingRepository(database.dataSource)
            ),
            "RegistrertDeltCv"
        )
        PresenterteOgFåttJobbenKandidaterLytter(
            this,
            LagreUtfallOgStilling(
                KandidatutfallRepository(database.dataSource),
                StillingRepository(database.dataSource)
            ),
            "RegistrertFåttJobben"
        )
        SendtTilArbeidsgiverKandidaterLytter(
            this,
            LagreUtfallOgStilling(
                KandidatutfallRepository(database.dataSource),
                StillingRepository(database.dataSource)
            )
        )

        Tiltaklytter(this, TiltaksRepository(database.dataSource))
        TiltakManglerAktørIdLytter(this)
    }

    statistikkjobb.start();
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

val objectMapper = defaultProperties(jacksonObjectMapper())

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
        val tiltaksRepository = TiltaksRepository(dataSource)

        routing {
            route("/rekrutteringsbistand-statistikk-api") {
                hentStatistikk(kandidatutfallRepository, tiltaksRepository)
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
fun nowOslo(): ZonedDateTime = ZonedDateTime.now().toOslo()

fun ZonedDateTime.toOslo(): ZonedDateTime = this.truncatedTo(MILLIS).withZoneSameInstant(of("Europe/Oslo"))

fun ZonedDateTime.toOsloSameLocal(): ZonedDateTime = this.truncatedTo(MILLIS).withZoneSameLocal(of("Europe/Oslo"))

fun LocalDateTime.atOslo(): ZonedDateTime = this.atZone(of("Europe/Oslo"))
fun Instant.atOslo(): ZonedDateTime = this.atZone(of("Europe/Oslo"))
