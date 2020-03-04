package no.nav.rekrutteringsbistand.statistikk

import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.application.lagApplicationEngine
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

@KtorExperimentalAPI
fun main() {
    val environment = Environment()
    val database = Database(environment)
    val applicationEngine = lagApplicationEngine(environment, database)
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${environment.miljø}")
}
