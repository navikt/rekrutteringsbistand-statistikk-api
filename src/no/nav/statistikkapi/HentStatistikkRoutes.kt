package no.nav.statistikkapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate
import java.time.LocalDateTime

data class HentStatistikk(
    val fra: LocalDateTime,
    val til: LocalDateTime,
    val navKontor: String
) {
    constructor(fraOgMed: LocalDate, tilOgMed: LocalDate, navKontor: String) : this(
        fra = fraOgMed.atStartOfDay(),
        til = tilOgMed.plusDays(1).atStartOfDay(),
        navKontor = navKontor
    )
}

class StatistikkParametere {
    companion object {
        const val fraOgMed = "fraOgMed"
        const val tilOgMed = "tilOgMed"
        const val navKontor = "navKontor"
    }
}

data class StatistikkOutboundDto(
    val antallPresentert: Int,
    val antallPresentertIPrioritertMålgruppe: Int,
    val antallFåttJobben: Int,
    val antallFåttJobbenIPrioritertMålgruppe: Int,
)

fun Route.hentStatistikk(kandidatutfallRepository: KandidatutfallRepository) {

    authenticate {
        get("/statistikk") {
            val queryParameters = call.parameters
            val fraOgMedParameter = queryParameters[StatistikkParametere.fraOgMed]
            val tilOgMedParameter = queryParameters[StatistikkParametere.tilOgMed]
            val navKontorParameter = queryParameters[StatistikkParametere.navKontor]

            if (fraOgMedParameter.isNullOrBlank() || tilOgMedParameter.isNullOrBlank() || navKontorParameter.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Alle parametere må ha verdi")
            } else {

                val hentStatistikk = HentStatistikk(
                    fraOgMed = LocalDate.parse(fraOgMedParameter),
                    tilOgMed = LocalDate.parse(tilOgMedParameter),
                    navKontor = navKontorParameter
                )

                val antallPresentasjoner = kandidatutfallRepository.hentAntallPresentasjoner(hentStatistikk)
                val antallPresentasjonerIPrioritertMålgruppe =
                    kandidatutfallRepository.hentAntallPresentasjonerIPrioritertMålgruppe(hentStatistikk)

                val fåttJobben = kandidatutfallRepository.hentAktoriderForFåttJobben(hentStatistikk)
                val fåttJobbenIPrioritertMålgruppe =
                    kandidatutfallRepository.hentAktoriderForFåttJobbenIPrioritertMålgruppe(hentStatistikk)

                call.respond(
                    StatistikkOutboundDto(
                        antallPresentasjoner,
                        antallPresentasjonerIPrioritertMålgruppe,
                        fåttJobben.size,
                        fåttJobbenIPrioritertMålgruppe.size
                    )
                )
            }
        }
    }
}


