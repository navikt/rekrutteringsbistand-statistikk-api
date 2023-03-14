package no.nav.statistikkapi.kafka

import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import no.nav.statistikkapi.tiltak.TiltaksRepository

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori) {
        log.info("Stubber sending av kandidatutfall til Datavarehus. Kandidatutfall: $kandidatutfall")
    }

    override fun send(tiltak: TiltaksRepository.Tiltak, stillingskategori: Stillingskategori) {
        log.info("Stubber sending av tiltak til Datavarehus. Tiltak: $tiltak")

    }
}
