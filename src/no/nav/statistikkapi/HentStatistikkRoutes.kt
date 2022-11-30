package no.nav.statistikkapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.tiltak.Tiltakstilfelle
import no.nav.statistikkapi.tiltak.Tiltakstype
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
    val antallFåttJobbenTiltak: Int,
    val antallFåttJobbenTiltakArbeidstrening: Int,
    val antallFåttJobbenTiltakLønnstilskudd: Int,
    val antallFåttJobbenTiltakMentorordning: Int,
    val antallFåttJobbenTiltakAndre: Int,
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
                val fåttJobbenTiltak: List<Tiltakstilfelle> = emptyList() // TODO: kandidatutfallRepository.hentAktøridFåttJobbenTiltak(hentStatistikk).distinct()

                val unikeInnslagTiltak: List<String> = fåttJobbenTiltak.map(Tiltakstilfelle::aktørId) - fåttJobben

                call.respond(
                    StatistikkOutboundDto(
                        antallPresentert,
                        fåttJobben.size,
                        unikeInnslagTiltak.size,
                        fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.ARBEIDSTRENING }.size,
                        fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.LØNNSTILSKUDD }.size,
                        fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.MENTORORDNING }.size,
                        fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.ANNET }.map(Tiltakstilfelle::aktørId).distinct().size,
                    )
                )
            }
        }
    }
}


