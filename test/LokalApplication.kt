import io.ktor.auth.Authentication
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import db.TestDatabase
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
    database: DatabaseInterface = TestDatabase(),
    port: Int = 8080
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
        tokenValidationConfig
    )
    applicationEngine.start()
    log.info("Applikasjon startet")
}
