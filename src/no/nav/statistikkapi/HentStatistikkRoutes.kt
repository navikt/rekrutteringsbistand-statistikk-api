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

data class AntallDto(
    val totalt: Int,
    val under30år: Int,
    val innsatsgruppeIkkeStandard: Int,
)


data class StatistikkOutboundDto(
    val antPresentasjoner: AntallDto,
    val antFåttJobben: AntallDto,
    val antallPresentert: Int, // TODO Are: Slett når frontend er oppdatert
    val antallPresentertIPrioritertMålgruppe: Int, // TODO Are: Slett når frontend er oppdatert
    val antallFåttJobben: Int, // TODO Are: Slett når frontend er oppdatert
    val antallFåttJobbenIPrioritertMålgruppe: Int, // TODO Are: Slett når frontend er oppdatert
)


fun Route.hentStatistikk(repo: KandidatutfallRepository) {
    authenticate {
        get("/statistikk") {
            val queryParameters = call.parameters
            val fraOgMedParameter = queryParameters[StatistikkParametere.fraOgMed]
            val tilOgMedParameter = queryParameters[StatistikkParametere.tilOgMed]
            val navKontorParameter = queryParameters[StatistikkParametere.navKontor]

            if (fraOgMedParameter.isNullOrBlank() || tilOgMedParameter.isNullOrBlank() || navKontorParameter.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Alle parametere må ha verdi")
            } else {

                val hentStatistikkParams = HentStatistikk(
                    fraOgMed = LocalDate.parse(fraOgMedParameter),
                    tilOgMed = LocalDate.parse(tilOgMedParameter),
                    navKontor = navKontorParameter
                )
                val antPresentasjoner = AntallDto(
                    totalt = repo.hentAntallPresentasjoner(hentStatistikkParams),
                    under30år = repo.hentAntallPresentasjonerUnder30År(hentStatistikkParams),
                    innsatsgruppeIkkeStandard = repo.hentAntallPresentasjonerInnsatsgruppeIkkeStandard(
                        hentStatistikkParams
                    ),
                )
                val antFåttJobben = AntallDto(
                    totalt = repo.hentAntallFåttJobben(hentStatistikkParams),
                    under30år = repo.hentAntallFåttJobbenUnder30År(hentStatistikkParams),
                    innsatsgruppeIkkeStandard = repo.hentAntallFåttJobbenInnsatsgruppeIkkeStandard(hentStatistikkParams),
                )

                call.respond(
                    StatistikkOutboundDto(
                        antPresentasjoner = antPresentasjoner,
                        antFåttJobben = antFåttJobben,

                        // TODO Are: Slett når frontend er oppdatert
                        antallPresentert = antPresentasjoner.totalt,
                        antallPresentertIPrioritertMålgruppe = antPresentasjoner.under30år + antPresentasjoner.innsatsgruppeIkkeStandard,
                        antallFåttJobben = antFåttJobben.totalt,
                        antallFåttJobbenIPrioritertMålgruppe = antFåttJobben.under30år + antFåttJobben.innsatsgruppeIkkeStandard
                    )
                )
            }
        }
    }
}


