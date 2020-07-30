import io.ktor.auth.Authentication
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.Database
import db.TestDatabaseImpl
import kafka.DatavarehusKafkaProducerStub
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
    database: Database = TestDatabaseImpl(),
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

    val applicationEngine = lagApplicationEngine(
        port,
        database,
        tokenValidationConfig,
        datavarehusKafkaProducer
    )
    applicationEngine.start()

    log.info("Applikasjon startet")
}
