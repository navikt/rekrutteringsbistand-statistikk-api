package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.micrometer.core.instrument.Metrics
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log

data class OpprettKandidatutfall(
    val aktørId: String,
    val utfall: String,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String
)

fun Route.kandidatutfall(repository: Repository) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()
            log.info("Mottok ${kandidatutfall.size} kandidatutfall")

            kandidatutfall.forEach {
                repository.lagreUtfall(it)
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall).increment()
            }

//            triggsendingtilkafka()

            call.respond(HttpStatusCode.Created)
        }
    }
}
