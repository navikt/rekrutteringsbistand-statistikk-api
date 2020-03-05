package no.nav.rekrutteringsbistand.statistikk

import io.ktor.auth.Authentication
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.security.token.support.test.FileResourceRetriever

@KtorExperimentalAPI
fun main() {
    log.info("Starter applikasjon lokalt")
    val database = TestDatabase()

    val tokenSupportConfig = TokenSupportConfig(
        IssuerConfig(
            name = "isso",
            discoveryUrl = "http://metadata",
            acceptedAudience = listOf("aud-localhost", "aud-isso"),
            cookieName = "isso-idtoken"
        )
    )
    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        tokenValidationSupport(
            config = tokenSupportConfig,
            resourceRetriever = FileResourceRetriever("/metadata.json", "/jwkset.json")
        )
    }

    val applicationEngine = lagApplicationEngine(
        database,
        tokenValidationConfig
    )
    applicationEngine.start()
    log.info("Applikasjon startet")
}