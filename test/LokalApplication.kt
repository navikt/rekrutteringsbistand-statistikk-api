import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.application.lagApplicationEngine
import no.nav.rekrutteringsbistand.statistikk.auth.TokenValidationUtil
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.rekrutteringsbistand.statistikk.log
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.test.FileResourceRetriever

@KtorExperimentalAPI
fun main() {
    log.info("Starter applikasjon lokalt")
    val database = TestDatabase()
    val issuerConfig = IssuerConfig(
        name = "isso",
        discoveryUrl = "http://metadata",
        acceptedAudience = listOf("aud-localhost", "aud-isso"),
        cookieName = "isso-idtoken"
    )
    val tokenValidationConfig = TokenValidationUtil.tokenValidationConfig(
        issuerConfig = issuerConfig,
        resourceRetriever = FileResourceRetriever("/metadata.json", "/jwkset.json")
    )

    val applicationEngine = lagApplicationEngine(
        database,
        tokenValidationConfig
    )
    applicationEngine.start()
    log.info("Applikasjon startet")
}
