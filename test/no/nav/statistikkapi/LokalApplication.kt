package no.nav.statistikkapi

import io.ktor.server.auth.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerStub
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
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

    val envs = System.getenv() + mapOf(
        "HTTP_PORT" to port.toString(),
        "NAIS_APP_NAME" to "rekrutteringsbistand-statistikk-api-test",
        "NAIS_NAMESPACE" to "test",
        "NAIS_CLUSTER_NAME" to "test"
    )

    val applicationEngine = lagRapidsApplication(
        envs,
        database.dataSource,
        tokenValidationConfig,
        datavarehusKafkaProducer,
        object : ElasticSearchKlient {
            override fun hentStilling(stillingUuid: String): ElasticSearchStilling = enElasticSearchStilling()
        }
    )
    applicationEngine.start()

    log.info("Applikasjon startet")
}
