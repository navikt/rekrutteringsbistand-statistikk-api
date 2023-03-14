package no.nav.statistikkapi.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import no.nav.statistikkapi.tiltak.TiltaksRepository

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
                log.warn(
                    "Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall. utfall=${it.utfall}, stillingsId=${it.stillingsId}",
                    e
                )
                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet", "antallSendtForsøk", it.antallSendtForsøk.toString()
                ).increment()
            }
        }
}

fun hentUsendteTiltakOgSendPåKafka(
    tiltaksRepository: TiltaksRepository,
    kafkaProducer: DatavarehusKafkaProducer
) = Runnable {
    tiltaksRepository.hentUsendteTiltak()
        .forEach {
            try {
                tiltaksRepository.registrerSendtForsøk(it)
                kafkaProducer.send(it, Stillingskategori.FORMIDLING)
                tiltaksRepository.registrerSomSendt(it)
            } catch (e: Exception) {
                log.warn(
                    "Prøvde å sende tiltaksmelding på Kafka til Datavarehus om et tiltak. avtaleId=${it.avtaleId}",
                    e
                )
                Metrics.counter(
                    "rekrutteringsbistand.statistikk.kafka.feilet",
                    "antallTiltakSendtForsøk",
                    it.antallSendtForsøk.toString()
                ).increment()
            }
        }
}
