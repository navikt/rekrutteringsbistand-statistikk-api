package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaTilDataverehusScheduler
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true) // TODO: Fjerne når kandidat-api og statistikk-api er i synk
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
    val tilretteleggingsbehov: List<String> = emptyList() // TODO: Gjøre om til liste av enums
)

fun Route.kandidatutfall(kandidatutfallRepository: KandidatutfallRepository, sendStatistikk: KafkaTilDataverehusScheduler) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()
            log.info("Mottok ${kandidatutfall.size} kandidatutfall")

            kandidatutfall.forEach {
                kandidatutfallRepository.lagreUtfall(it, LocalDateTime.now())
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall.name).increment()
            }

            sendStatistikk.kjørEnGangAsync()
            call.respond(HttpStatusCode.Created)
        }
    }
}

