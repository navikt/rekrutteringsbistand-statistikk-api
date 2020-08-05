package no.nav.rekrutteringsbistand.statistikk.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log

fun hentUsendteUtfallOgSendPåKafka(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
//    val skalSendes = repository.hentUsendteUtfall()
//
//    skalSendes.forEach {
//        repository.registrerSendtForsøk(it)
//        try {
//            kafkaProducer.send(it)
//            repository.registrerSomSendt(it)
//        } catch (e: Exception) {
//            log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
//            Metrics.counter(
//                "rekrutteringsbistand.statistikk.kafka.feilet",
//                "antallSendtForsøk", it.antallSendtForsøk.toString()
//            ).increment()
//            return@Runnable
//        }
//    }
}
