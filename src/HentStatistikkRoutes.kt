package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*

data class StatistikkDto(
    val antallPresentert: Number,
    val antallFåttJobben: Number
)

fun Route.hentStatistikk() {

    authenticate {
        get("/statistikk") {
            call.respond(StatistikkDto(1, 5))
        }
    }
}
