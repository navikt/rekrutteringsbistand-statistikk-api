package no.nav.rekrutteringsbistand.statistikk.kafka

import io.micrometer.core.instrument.Metrics
import no.finn.unleash.Unleash
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import no.nav.rekrutteringsbistand.statistikk.unleash.SEND_KANDIDATUTFALL_PÅ_KAFKA

fun hentUsendteUtfallOgSendPåKafka(
    repository: Repository,
    kafkaProducer: DatavarehusKafkaProducer,
    unleash: Unleash
) = Runnable {
    val skalSendePåKafka = unleash.isEnabled(SEND_KANDIDATUTFALL_PÅ_KAFKA, true)
    if (skalSendePåKafka) {

        val skalSendes = repository.hentUsendteUtfall()
        skalSendes.forEach {
            repository.registrerSendtForsøk(it)
            try {
                kafkaProducer.send(it)
                repository.registrerSomSendt(it)
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

