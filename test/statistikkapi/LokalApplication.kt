package statistikkapi
import io.ktor.auth.*
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever
import statistikkapi.db.TestDatabase
import statistikkapi.kafka.DatavarehusKafkaProducer
import statistikkapi.kafka.DatavarehusKafkaProducerStub
import statistikkapi.stillinger.ElasticSearchKlient
import statistikkapi.stillinger.ElasticSearchStilling

fun main() {
    start()
}

fun start(
    database: TestDatabase = TestDatabase(),
    port: Int = 8111,
    datavarehusKafkaProducer: DatavarehusKafkaProducer = DatavarehusKafkaProducerStub()
) {
    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        val tokenSupportConfig = TokenSupportConfig(
            IssuerConfig(
                name = "isso",
                discoveryUrl = "http://metadata",
                acceptedAudience = listOf("aud-localhost", "aud-isso"),
            )
        )

        tokenValidationSupport(
            config = tokenSupportConfig,
            resourceRetriever = FileResourceRetriever("/metadata.json", "/jwkset.json")
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
