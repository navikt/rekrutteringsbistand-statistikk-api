package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.rekrutteringsbistand.statistikk.utils.LOG

data class Kandidatutfall(
    val aktorid: Number,
    val utfall: String,
    val navident: String,
    val enhetsnr: Number,
    val tidspunkt: String
)

fun Route.kandidatutfall() {
    post("/kandidatutfall") {
        val kandidatstatus = call.receive<Kandidatutfall>()
        LOG.info("Kandidatstatus post: \n${kandidatstatus}")
        call.respond(HttpStatusCode.OK)
    }
}
