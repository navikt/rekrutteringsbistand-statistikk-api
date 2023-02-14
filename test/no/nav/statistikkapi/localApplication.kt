package no.nav.statistikkapi

import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.tiltak.TiltakManglerAktørIdLytter
import no.nav.statistikkapi.tiltak.Tiltaklytter
import no.nav.statistikkapi.tiltak.TiltaksRepository
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

    Tiltaklytter(rapid, TiltaksRepository(database.dataSource))
    TiltakManglerAktørIdLytter(rapid)

    PresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        "RegistrertDeltCv"
    )
    PresenterteOgFåttJobbenKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        ),
        "RegistrertFåttJobben"
    )
    SendtTilArbeidsgiverKandidaterLytter(
        rapid,
        LagreUtfallOgStilling(
            KandidatutfallRepository(database.dataSource),
            StillingRepository(database.dataSource)
        )
    )

    Kandidathendelselytter(
        rapid,
        KandidatutfallRepository(database.dataSource),
        StillingRepository(database.dataSource)
    )


    val ktorServer = embeddedServer(CIO, port = port) {}
    val ktorApplication = ktorServer.application

    settOppKtor(
        ktorApplication,
        tokenValidationConfig,
        database.dataSource
    )

    ktorServer.start()
    log.info("Applikasjon startet")
}
