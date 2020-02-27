package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.rekrutteringsbistand.statistikk.utils.Log
import java.time.LocalDateTime

data class Kandidatutfall(
        val aktørId: String,
        val utfall: String,
        val navIdent: String,
        val enhetsnr: String,
        val tidspunkt: LocalDateTime
)

fun Route.kandidatutfall() {

    post("/kandidatutfall") {
        val kandidatstatusListe = call.receive<List<Kandidatutfall>>()
        Log.info("Kandidatstatusliste post: \n${kandidatstatusListe}")
        call.respond(HttpStatusCode.NotImplemented)
    }
}
