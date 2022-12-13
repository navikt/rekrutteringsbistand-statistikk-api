package no.nav.statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingRepository

fun hentUsendteUtfallOgSendPåKafka(
    kandidatutfallRepository: KandidatutfallRepository,
    kafkaProducer: DatavarehusKafkaProducer,
    stillingRepository: StillingRepository
) = Runnable {
    kandidatutfallRepository.hentUsendteUtfall()
        .forEach {
            try {
                kandidatutfallRepository.registrerSendtForsøk(it)
                val stilling = stillingRepository.hentStilling(it.stillingsId)
                kafkaProducer.send(it, stilling!!.stillingskategori)
                kandidatutfallRepository.registrerSomSendt(it)
            } catch (e: Exception) {
                log.warn("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet", "antallSendtForsøk", it.antallSendtForsøk.toString()
                ).increment()
            }
        }
}
