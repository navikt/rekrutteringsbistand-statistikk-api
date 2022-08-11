package no.nav.statistikkapi

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerStub
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.ElasticSearchStilling
import no.nav.statistikkapi.stillinger.StillingRepository
import java.net.InetAddress
import javax.sql.DataSource

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

    val ktor = embeddedServer(Netty, port = port) {}
    val ktorApplication = ktor.application


    startAppLocal(
        database.dataSource,
        tokenValidationConfig,
        datavarehusKafkaProducer,
        object : ElasticSearchKlient {
            override fun hentStilling(stillingUuid: String): ElasticSearchStilling = enElasticSearchStilling()
        },
        ktorApplication,
        TestRapid()
    )
    ktor.start()
    log.info("Applikasjon startet")
}

fun startAppLocal(
    dataSource: DataSource,
    tokenValidationConfig: AuthenticationConfig.() -> Unit,
    datavarehusKafkaProducer: DatavarehusKafkaProducer,
    elasticSearchKlient: ElasticSearchKlient,
    ktor: Application?,
    rapidsConnection: RapidsConnection
) {
    Kandidathendelselytter(rapidsConnection)

    ktor!!.apply {
        settOppKtor(this, tokenValidationConfig, dataSource, elasticSearchKlient, datavarehusKafkaProducer)
    }

    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
