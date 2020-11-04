package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import java.time.LocalDate

data class StatistikkInboundDto(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val navkontor: String
)

data class StatistikkOutboundDto(
    val antallPresentert: Int,
    val antallFåttJobben: Int
)

fun Route.hentStatistikk(repository: Repository) {

    authenticate {
        get("/statistikk") {
            val inboundDto: StatistikkInboundDto = call.receive()

            val antallPresentert =
                repository.hentAntallPresentert(inboundDto.fraOgMed, inboundDto.tilOgMed, inboundDto.navkontor)
            val antallFåttJobben =
                repository.hentAntallFåttJobben(inboundDto.fraOgMed, inboundDto.tilOgMed, inboundDto.navkontor)

            call.respond(StatistikkOutboundDto(antallPresentert, antallFåttJobben))
        }
    }
}
