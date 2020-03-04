package no.nav.rekrutteringsbistand.statistikk

import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.application.lagApplicationEngine
import no.nav.rekrutteringsbistand.statistikk.auth.TokenValidationUtil
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

@KtorExperimentalAPI
fun main() {
    val environment = Environment()
    val database = Database(environment.cluster)

    val issuerConfig = TokenValidationUtil.issuerConfig(environment.cluster)
    val tokenValidationConfig = TokenValidationUtil.tokenValidationConfig(issuerConfig = issuerConfig)

    val applicationEngine = lagApplicationEngine(
        database,
        tokenValidationConfig
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${environment.cluster}")
}
