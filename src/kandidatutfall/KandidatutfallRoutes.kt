package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaTilDataverehusScheduler
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDateTime

data class OpprettKandidatutfall(
    val aktørId: String,
    val utfall: String,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String,
    val harHullICv: Boolean?
)

fun Route.kandidatutfall(repository: Repository, sendStatistikk: KafkaTilDataverehusScheduler) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()
            log.info("Mottok ${kandidatutfall.size} kandidatutfall")

            kandidatutfall.forEach {
                repository.lagreUtfall(it, LocalDateTime.now())
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall).increment()
            }

            sendStatistikk.kjørEnGangAsync()
            call.respond(HttpStatusCode.Created)
        }
    }
}
