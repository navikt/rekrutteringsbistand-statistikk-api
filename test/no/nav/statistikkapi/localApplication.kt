package no.nav.statistikkapi

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatliste.KandidatlistehendelseLytter
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoLytter
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import org.slf4j.LoggerFactory
import java.net.InetAddress

fun main() {
    start()
}

fun start(
    database: TestDatabase = TestDatabase(),
    port: Int = 8080,
    mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    rapid: RapidsConnection = TestRapid()
) {
    val mockOAuth2ServerPort = randomPort()
    mockOAuth2Server.start(InetAddress.getByName("localhost"), mockOAuth2ServerPort)

    val tokenSupportConfig = TokenSupportConfig(
        IssuerConfig(
            name = "azuread",
            discoveryUrl = "http://localhost:$mockOAuth2ServerPort/azuread/.well-known/openid-configuration",
            acceptedAudience = listOf("statistikk-api")
        )
    )

    val tokenValidationConfig: AuthenticationConfig.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    PresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        "RegistrertDeltCv",
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    PresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        "RegistrertFåttJobben",
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    ReverserPresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        utfallRepository = KandidatutfallRepository(database.dataSource),
        "FjernetRegistreringDeltCv",
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    ReverserPresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        utfallRepository = KandidatutfallRepository(database.dataSource),
        "FjernetRegistreringFåttJobben",
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    SendtTilArbeidsgiverKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    SlettetStillingOgKandidatlisteLytter(
        rapidsConnection = rapid,
        repository = KandidatutfallRepository(database.dataSource),
        prometheusMeterRegistry = prometheusMeterRegistry,
        lagreUtfallOgStilling = LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        )
    )
    KandidatlistehendelseLytter(
        rapidsConnection = rapid,
        repository = KandidatlisteRepository(database.dataSource)
    )
    VisningKontaktinfoLytter(
        rapidsConnection = rapid,
        repository = VisningKontaktinfoRepository(database.dataSource)
    )

    val ktorServer = embeddedServer(CIO, port = port) {}
    val ktorApplication = ktorServer.application

    settOppKtor(
        ktorApplication,
        tokenValidationConfig,
        database.dataSource,
        prometheusMeterRegistry
    )

    ktorServer.start()

    val loggerWithoutClassname = LoggerFactory.getLogger("no.nav.statistikkapi.start")
    loggerWithoutClassname.info("Applikasjon startet")
}
