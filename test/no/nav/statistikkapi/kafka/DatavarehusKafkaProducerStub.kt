package no.nav.statistikkapi.kafka

import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.log

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall) {
        log.info("Stubber sending av kandidatutfall til Datavarehus. Kandidatutfall: $kandidatutfall")
    }
}
