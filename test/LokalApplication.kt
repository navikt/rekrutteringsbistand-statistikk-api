
import db.TestDatabase
import io.ktor.auth.*
import io.ktor.util.*
import kafka.DatavarehusKafkaProducerStub
import no.finn.unleash.FakeUnleash
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogUrl
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.lagApplicationEngine
import no.nav.rekrutteringsbistand.statistikk.log
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever

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
