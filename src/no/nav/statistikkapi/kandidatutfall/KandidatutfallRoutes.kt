package no.nav.statistikkapi.kandidatutfall

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import no.nav.statistikkapi.log
import no.nav.statistikkapi.toOslo
import java.time.ZonedDateTime

data class OpprettKandidatutfall(
    val aktørId: String,
    val utfall: Utfall,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String,
    val synligKandidat: Boolean,
    val harHullICv: Boolean?,
    val alder: Int?,
    val tilretteleggingsbehov: List<String>,
    val tidspunktForHendelsen: ZonedDateTime,
)

fun Route.kandidatutfall(
    kandidatutfallRepository: KandidatutfallRepository,
    sendStatistikk: KafkaTilDataverehusScheduler,
) {

    authenticate {
        post("/kandidatutfall") {
            log.info("Tar i mot kandidatutfall")
            val kandidatutfall: List<OpprettKandidatutfall> = call.receive<Array<OpprettKandidatutfall>>()
                .map {
                    it.copy(tidspunktForHendelsen = it.tidspunktForHendelsen.toOslo())
                }

            kandidatutfall.forEach {
                kandidatutfallRepository.lagreUtfallIdempotent(it)
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall.name).increment()
            }
            sendStatistikk.kjørEnGangAsync()
            call.respond(HttpStatusCode.Created)
        }
    }
}
