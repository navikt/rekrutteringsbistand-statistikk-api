package kafka

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.log

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall) {
        log.info("Stubber sending av kandidatutfall til Datavarehus. Kandidatutfall: $kandidatutfall")
    }
}
