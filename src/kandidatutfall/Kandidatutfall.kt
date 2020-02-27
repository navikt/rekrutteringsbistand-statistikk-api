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
        val kandidatstatus = call.receive<Kandidatutfall>()
        Log.info("Kandidatstatus post: \n${kandidatstatus}")
        call.respond(HttpStatusCode.OK)
    }
}
