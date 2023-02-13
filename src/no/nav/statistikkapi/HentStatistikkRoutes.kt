package no.nav.statistikkapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.tiltak.TiltaksRepository
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
    val tiltakstatistikk: TiltakStatistikkDto
)

data class TiltakStatistikkDto(
    val antallFåttJobben: Int,
    val antallFåttJobbenArbeidstrening: Int,
    val antallFåttJobbenLønnstilskudd: Int,
    val antallFåttJobbenMentorordning: Int,
    val antallFåttJobbenAndreTiltak: Int,
)

fun Route.hentStatistikk(kandidatutfallRepository: KandidatutfallRepository, tiltaksRepository: TiltaksRepository) {

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
                val fåttJobben = kandidatutfallRepository.hentAktoriderForFåttJobben(hentStatistikk)
                val fåttJobbenTiltak = tiltaksRepository.hentAktøridFåttJobbenTiltak(hentStatistikk)

                call.respond(
                    StatistikkOutboundDto(
                        antallPresentert,
                        fåttJobben.size,
                        TiltakStatistikkDto(
                            antallFåttJobben = fåttJobbenTiltak.size,
                            antallFåttJobbenArbeidstrening = fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.ARBEIDSTRENING }.size,
                            antallFåttJobbenLønnstilskudd = fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.LØNNSTILSKUDD }.size,
                            antallFåttJobbenMentorordning = fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.MENTORORDNING }.size,
                            antallFåttJobbenAndreTiltak = fåttJobbenTiltak.filter { it.tiltakstype == Tiltakstype.ANNET }
                                .map(Tiltakstilfelle::aktørId).distinct().size,
                        )
                    )
                )
            }
        }
    }
}


