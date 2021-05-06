package statistikkapi
import io.ktor.auth.*
import io.ktor.util.*
import no.finn.unleash.FakeUnleash
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever
import statistikkapi.datakatalog.DatakatalogUrl
import statistikkapi.db.TestDatabase
import statistikkapi.kafka.DatavarehusKafkaProducer
import statistikkapi.kafka.DatavarehusKafkaProducerStub

@KtorExperimentalAPI
fun main() {
    start()
}

@KtorExperimentalAPI
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
                cookieName = "isso-idtoken"
            )
        )

        tokenValidationSupport(
            config = tokenSupportConfig,
            resourceRetriever = FileResourceRetriever("/metadata.json", "/jwkset.json")
        )
    }

    val unleash = FakeUnleash().apply {
        enableAll()
    }

    val applicationEngine = lagApplicationEngine(
        port,
        database.dataSource,
        tokenValidationConfig,
        datavarehusKafkaProducer,
        unleash,
        DatakatalogUrl(Cluster.LOKAL)
    )
    applicationEngine.start()

    log.info("Applikasjon startet")
}
