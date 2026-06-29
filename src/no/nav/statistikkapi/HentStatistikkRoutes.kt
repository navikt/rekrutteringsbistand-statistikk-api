package no.nav.statistikkapi

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.statistikkapi.kandidatutfall.AntallFåttJobben
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.LocalDate
import java.time.LocalDateTime

data class StatistikkForespørsel(
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

object StatistikkParameternavn {
    const val fraOgMed = "fraOgMed"
    const val tilOgMed = "tilOgMed"
    const val navKontor = "navKontor"
}

data class AntallDto(
    val totalt: Int,
    val under30år: Int,
    val innsatsgruppeIkkeStandard: Int,
)

private fun AntallFåttJobben.tilDto() = AntallDto(
    totalt = totalt,
    under30år = under30år,
    innsatsgruppeIkkeStandard = innsatsgruppeIkkeStandard,
)


data class FåttJobbenPerKategoriDto(
    val stilling: AntallDto,
    val rekrutteringstreff: AntallDto,
    val etterregistrering: AntallDto,
)


data class StatistikkOutboundDto(
    val antPresentasjoner: AntallDto,
    val antFåttJobben: AntallDto,
    val fåttJobbenPerKategori: FåttJobbenPerKategoriDto,
)


fun Route.hentStatistikk(repo: KandidatutfallRepository) {
    authenticate {
        get("/statistikk") {
            val queryParameters = call.parameters
            val fraOgMedParameter = queryParameters[StatistikkParameternavn.fraOgMed]
            val tilOgMedParameter = queryParameters[StatistikkParameternavn.tilOgMed]
            val navKontorParameter = queryParameters[StatistikkParameternavn.navKontor]

            if (fraOgMedParameter.isNullOrBlank() || tilOgMedParameter.isNullOrBlank() || navKontorParameter.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Alle parametere må ha verdi")
            } else {

                val forespørsel = StatistikkForespørsel(
                    fraOgMed = LocalDate.parse(fraOgMedParameter),
                    tilOgMed = LocalDate.parse(tilOgMedParameter),
                    navKontor = navKontorParameter
                )
                val antPresentasjoner = AntallDto(
                    totalt = repo.hentAntallPresentasjoner(forespørsel),
                    under30år = repo.hentAntallPresentasjonerUnder30År(forespørsel),
                    innsatsgruppeIkkeStandard = repo.hentAntallPresentasjonerInnsatsgruppeIkkeStandard(
                        forespørsel
                    ),
                )
                val antFåttJobben = AntallDto(
                    totalt = repo.hentAntallFåttJobben(forespørsel),
                    under30år = repo.hentAntallFåttJobbenUnder30År(forespørsel),
                    innsatsgruppeIkkeStandard = repo.hentAntallFåttJobbenInnsatsgruppeIkkeStandard(forespørsel),
                )

                val fåttJobbenPerKategori = FåttJobbenPerKategoriDto(
                    stilling = repo.hentAntallFåttJobben(forespørsel, Stillingskategori.STILLING).tilDto(),
                    rekrutteringstreff = repo.hentAntallFåttJobben(
                        forespørsel,
                        Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING
                    ).tilDto(),
                    etterregistrering = repo.hentAntallFåttJobben(forespørsel, Stillingskategori.FORMIDLING).tilDto(),
                )

                call.respond(
                    StatistikkOutboundDto(
                        antPresentasjoner = antPresentasjoner,
                        antFåttJobben = antFåttJobben,
                        fåttJobbenPerKategori = fåttJobbenPerKategori,
                    )
                )
            }
        }
    }
}


