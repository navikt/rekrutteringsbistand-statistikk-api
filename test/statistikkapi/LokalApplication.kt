package statistikkapi
import io.ktor.auth.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import statistikkapi.db.TestDatabase
import statistikkapi.kafka.DatavarehusKafkaProducer
import statistikkapi.kafka.DatavarehusKafkaProducerStub
import statistikkapi.stillinger.ElasticSearchKlient
import statistikkapi.stillinger.ElasticSearchStilling
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

    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        val tokenSupportConfig = TokenSupportConfig(
            IssuerConfig(
                name = "azuread",
                discoveryUrl = "http://localhost:$mockOAuth2ServerPort/azuread/.well-known/openid-configuration",
                acceptedAudience = listOf("statistikk-api")
            )
        )

        tokenValidationSupport(
            config = tokenSupportConfig
        )
    }

    val applicationEngine = lagApplicationEngine(
        port,
        database.dataSource,
        tokenValidationConfig,
        datavarehusKafkaProducer,
        object: ElasticSearchKlient {
            override fun hentStilling(stillingUuid: String): ElasticSearchStilling = enElasticSearchStilling()
        }
    )
    applicationEngine.start()

    log.info("Applikasjon startet")
}
