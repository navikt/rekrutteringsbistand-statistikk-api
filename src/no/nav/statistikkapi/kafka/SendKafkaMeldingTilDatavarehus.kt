package no.nav.statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingService

fun hentUsendteUtfallOgSendPåKafka(
    kandidatutfallRepository: KandidatutfallRepository,
    kafkaProducer: DatavarehusKafkaProducer,
    stillingService: StillingService
) = Runnable {
    kandidatutfallRepository
        .hentUsendteUtfall()
        .forEach {
            try {
                kandidatutfallRepository.registrerSendtForsøk(it)
                stillingService.registrerStilling(it.stillingsId)
                kafkaProducer.send(it)
                kandidatutfallRepository.registrerSomSendt(it)
            } catch (e: Exception) {
                log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet",
                    "antallSendtForsøk", it.antallSendtForsøk.toString()
                ).increment()
                return@Runnable
            }
        }
}

