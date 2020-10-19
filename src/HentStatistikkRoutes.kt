package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Utfall.*
import java.time.LocalDate

data class StatistikkInboundDto(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

data class StatistikkOutboundDto(
    val antallPresentert: Int,
    val antallFåttJobben: Int
)

fun Route.hentStatistikk(repository: Repository) {

    authenticate {
        get("/statistikk") {
            val inboundDto: StatistikkInboundDto = call.receive()

            val antallPresentert = repository.hentAntallPresentert(inboundDto.fraOgMed, inboundDto.tilOgMed)
            val antallFåttJobben = repository.hentAntallFåttJobben(inboundDto.fraOgMed, inboundDto.tilOgMed)

            call.respond(StatistikkOutboundDto(antallPresentert, antallFåttJobben))
        }
    }
}
