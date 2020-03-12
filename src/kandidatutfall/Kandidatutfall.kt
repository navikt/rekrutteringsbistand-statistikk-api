package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.rekrutteringsbistand.statistikk.log

data class Kandidatutfall(
    val aktørId: String,
    val utfall: String,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String
)

fun Route.kandidatutfall() {

    authenticate {
        post("/kandidatutfall") {
            val kandidatstatusListe = call.receive<List<Kandidatutfall>>()
            log.info("Kandidatstatusliste post: \n${kandidatstatusListe}")
            call.respond(HttpStatusCode.Created)
        }
    }
}
