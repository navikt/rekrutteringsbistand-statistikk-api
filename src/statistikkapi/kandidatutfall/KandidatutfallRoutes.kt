package no.nav.statistikkapi.kandidatutfall

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.log
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import no.nav.statistikkapi.stillinger.StillingService
import java.time.LocalDateTime
import kotlin.concurrent.thread

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

fun Route.kandidatutfall(kandidatutfallRepository: KandidatutfallRepository, sendStatistikk: KafkaTilDataverehusScheduler, stillingService: StillingService) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()

            thread {
                try {
                    kandidatutfall.map { it.stillingsId }.distinct().forEach {
                        stillingService.registrerStilling(it)
                    }
                } catch (e: Exception) {
                    log.error("Kunne ikke registrere stilling: $e", e)
                }
            }

            kandidatutfall.forEach {
                kandidatutfallRepository.lagreUtfall(it, LocalDateTime.now())
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall.name).increment()
            }

            sendStatistikk.kjørEnGangAsync()
            call.respond(HttpStatusCode.Created)
        }
    }
}
