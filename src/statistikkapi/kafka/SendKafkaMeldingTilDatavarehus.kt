package statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.finn.unleash.Unleash
import statistikkapi.log
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.unleash.SEND_KANDIDATUTFALL_PÅ_KAFKA

fun hentUsendteUtfallOgSendPåKafka(
    kandidatutfallRepository: KandidatutfallRepository,
    kafkaProducer: DatavarehusKafkaProducer,
    unleash: Unleash
) = Runnable {
    val skalSendePåKafka = unleash.isEnabled(SEND_KANDIDATUTFALL_PÅ_KAFKA)
    if (skalSendePåKafka) {

        val skalSendes = kandidatutfallRepository.hentUsendteUtfall()
        skalSendes.forEach {
            kandidatutfallRepository.registrerSendtForsøk(it)
            try {
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

    } else {
        return@Runnable
    }
}

