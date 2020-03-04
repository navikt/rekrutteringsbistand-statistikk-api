import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.application.lagApplicationEngine
import no.nav.rekrutteringsbistand.statistikk.db.TestDatabase
import no.nav.rekrutteringsbistand.statistikk.log
import no.nav.rekrutteringsbistand.statistikk.utils.Environment

@KtorExperimentalAPI
fun main() {
    val environment = Environment()
    val database = TestDatabase()
    val applicationEngine = lagApplicationEngine(environment, database)
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${environment.miljø}")
}
