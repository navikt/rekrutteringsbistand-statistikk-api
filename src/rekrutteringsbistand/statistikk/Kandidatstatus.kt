package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.LOG
import java.time.LocalDateTime

fun Route.kandidatstatus() {
    route("/kandidatstatus") {
        post("/" ) {
            val kandidatstatus = call.receive<Kandidatstatus>()
            LOG.info("Kandidatstatus post: \n${kandidatstatus}")
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class Kandidatstatus(
        val aktorid: Number,
        val utfall: String,
        val navident: String,
        val enhetsnr: Number,
        val tidspunkt: String
)
