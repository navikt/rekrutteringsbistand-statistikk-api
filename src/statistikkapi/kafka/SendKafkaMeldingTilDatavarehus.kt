package statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import statistikkapi.log
import statistikkapi.kandidatutfall.KandidatutfallRepository

fun hentUsendteUtfallOgSendPåKafka(
    kandidatutfallRepository: KandidatutfallRepository,
    kafkaProducer: DatavarehusKafkaProducer
) = Runnable {
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
}

