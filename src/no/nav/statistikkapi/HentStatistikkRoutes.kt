package no.nav.statistikkapi

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

data class HentStatistikk(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val navKontor: String
)

class StatistikkParametere {
    companion object {
        const val fraOgMed = "fraOgMed"
        const val tilOgMed = "tilOgMed"
        const val navKontor = "navKontor"
    }
}

data class StatistikkOutboundDto(
    val antallPresentert: Int,
    val antallFåttJobben: Int,
    val antallFåttJobbenTiltak: Int
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
                    tilOgMed = LocalDate.parse(tilOgMedParameter).plusDays(1),
                    navKontor = navKontorParameter
                )

                val antallPresentert = kandidatutfallRepository.hentAntallPresentert(hentStatistikk)
                val fåttJobben = kandidatutfallRepository.hentAktoridFåttJobben(hentStatistikk).distinct()
                val fåttJobbenTiltak =  kandidatutfallRepository.hentAktøridFåttJobbenTiltak(hentStatistikk).distinct()

                val unikeInnslagTiltak: List<String> = fåttJobbenTiltak-fåttJobben

                call.respond(StatistikkOutboundDto(antallPresentert, fåttJobben.size, unikeInnslagTiltak.size))
            }
        }
    }
}


