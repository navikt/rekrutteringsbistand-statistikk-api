package no.nav.statistikkapi

import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatliste.KandidatlistehendelseLytter
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.tiltak.TiltakManglerAktørIdLytter
import no.nav.statistikkapi.tiltak.Tiltaklytter
import no.nav.statistikkapi.tiltak.TiltaksRepository
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoLytter
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import java.net.InetAddress

fun main() {
    start()
}

fun start(
    database: TestDatabase = TestDatabase(),
    port: Int = 8111,
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
        repository =  KandidatutfallRepository(database.dataSource),
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

    Tiltaklytter(rapid, TiltaksRepository(database.dataSource))
    TiltakManglerAktørIdLytter(rapid)


    val ktorServer = embeddedServer(CIO, port = port) {}
    val ktorApplication = ktorServer.application

    settOppKtor(
        ktorApplication,
        tokenValidationConfig,
        database.dataSource,
        prometheusMeterRegistry
    )

    ktorServer.start()
    logWithoutClassname.info("Applikasjon startet")
}
