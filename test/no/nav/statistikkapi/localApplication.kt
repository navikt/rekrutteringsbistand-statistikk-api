package no.nav.statistikkapi

import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerStub
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.stillinger.StillingEsKlient
import no.nav.statistikkapi.stillinger.ElasticSearchStilling
import java.net.InetAddress

fun main() {
    start()
}

fun start(
    database: TestDatabase = TestDatabase(),
    port: Int = 8111,
    datavarehusKafkaProducer: DatavarehusKafkaProducer = DatavarehusKafkaProducerStub(),
    mockOAuth2Server: MockOAuth2Server = MockOAuth2Server()
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

    val rapid = TestRapid()
    Kandidathendelselytter(rapid)
    val stillingEsKlient = object : StillingEsKlient {
        override fun hentStilling(stillingUuid: String): ElasticSearchStilling = enElasticSearchStilling()
    }

    val ktorServer = embeddedServer(CIO, port = port) {}
    val ktorApplication = ktorServer.application

    settOppKtor(
        ktorApplication,
        tokenValidationConfig,
        database.dataSource,
        stillingEsKlient,
        datavarehusKafkaProducer
    )

    ktorServer.start()
    log.info("Applikasjon startet")
}
