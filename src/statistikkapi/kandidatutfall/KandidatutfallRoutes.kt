package statistikkapi.kandidatutfall

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.launch
import statistikkapi.log
import statistikkapi.kafka.KafkaTilDataverehusScheduler
import statistikkapi.stillinger.StillingService
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
            val start = System.currentTimeMillis()

            var start2 = System.currentTimeMillis();
            val kandidatutfall: Array<OpprettKandidatutfall> = call.receive()
            log.info("Mottok ${kandidatutfall.size} kandidatutfall, tok {} ms", System.currentTimeMillis() - start2)

            start2 = System.currentTimeMillis();
            thread {
                try {
                    kandidatutfall.map { it.stillingsId }.distinct().forEach {
                        stillingService.registrerStilling(it)
                    }
                } catch (e: Exception) {
                    log.error("Kunne ikke registrere stilling", e)
                }
            }
            log.info("Mottok kandidatutfall, registrerte stillinger, tok {} ms", System.currentTimeMillis() - start2)

            start2 = System.currentTimeMillis();
            kandidatutfall.forEach {
                kandidatutfallRepository.lagreUtfall(it, LocalDateTime.now())
                Metrics.counter("rekrutteringsbistand.statistikk.utfall.lagret", "utfall", it.utfall.name).increment()
            }
            log.info("Mottok kandidatutfall, lagre utfall: {} ms", System.currentTimeMillis() - start2);

            start2 = System.currentTimeMillis();
            sendStatistikk.kjørEnGangAsync()
            log.info("Mottok kandidatutfall, send statistikk: {} ms", System.currentTimeMillis() - start2);

            call.respond(HttpStatusCode.Created)

            log.info("Mottok kandidatutfall, hele kallet tok {} ms", System.currentTimeMillis() - start)
        }
    }
}

