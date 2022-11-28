package no.nav.statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.Stilling
import no.nav.statistikkapi.stillinger.StillingService
import java.util.*

fun hentUsendteUtfallOgSendPåKafka(
    kandidatutfallRepository: KandidatutfallRepository,
    kafkaProducer: DatavarehusKafkaProducer,
    stillingService: StillingService
) = Runnable {
    val harBlittRegistrertIDenneKjøringa: MutableMap<UUID, Stilling> = mutableMapOf()

    kandidatutfallRepository.hentUsendteUtfall()
        .forEach {
            try {
                kandidatutfallRepository.registrerSendtForsøk(it)
                val stilling = harBlittRegistrertIDenneKjøringa.computeIfAbsent(it.stillingsId, stillingService::registrerOgHent)
                kafkaProducer.send(it, stilling.stillingskategori)
                kandidatutfallRepository.registrerSomSendt(it)
            } catch (e: Exception) {
                log.warn("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet", "antallSendtForsøk", it.antallSendtForsøk.toString()
                ).increment()
            }
        }
}
