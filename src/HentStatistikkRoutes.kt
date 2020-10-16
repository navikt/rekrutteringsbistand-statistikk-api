package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.LocalDate

data class StatistikkInboundDto(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

data class StatistikkOutboundDto(
    val antallPresentert: Number,
    val antallFåttJobben: Number
)

fun Route.hentStatistikk() {

    authenticate {
        get("/statistikk") {
            call.respond(StatistikkOutboundDto(1, 5))
        }
    }
}
