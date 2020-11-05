package no.nav.rekrutteringsbistand.statistikk

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
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
    val antallFåttJobben: Int
)

fun Route.hentStatistikk(repository: Repository) {

    authenticate {
        get("/statistikk") {
            val queryParameters = call.parameters;
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

                val antallPresentert =
                    repository.hentAntallPresentert(hentStatistikk)
                val antallFåttJobben =
                    repository.hentAntallFåttJobben(hentStatistikk)

                call.respond(StatistikkOutboundDto(antallPresentert, antallFåttJobben))
            }
        }
    }
}


