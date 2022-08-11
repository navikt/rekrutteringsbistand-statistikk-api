package no.nav.statistikkapi.kandidatutfall

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import java.time.LocalDateTime

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
    val tilretteleggingsbehov: List<String>
)

fun Route.kandidatutfall(
    kandidatutfallRepository: KandidatutfallRepository,
    sendStatistikk: KafkaTilDataverehusScheduler,
) {

    authenticate {
        post("/kandidatutfall") {
            log.info("Tar i mot kandidatutfall")
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()
            kandidatutfall.forEach {
                kandidatutfallRepository.lagreUtfall(it, LocalDateTime.now())
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall.name).increment()
            }
            sendStatistikk.kjørEnGangAsync()
            call.respond(HttpStatusCode.Created)
        }
    }
}
