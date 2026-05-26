package no.nav.statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.logging.noClassLogger
import no.nav.statistikkapi.stillinger.StillingRepository

private val log = noClassLogger()

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
                val msg =
                    "Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall. utfall=${it.utfall}, stillingsId=${it.stillingsId}"
                log.warn(msg, e)

                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet", "antallSendtForsøk", it.antallSendtForsøk.toString()
                ).increment()
            }
        }
}
