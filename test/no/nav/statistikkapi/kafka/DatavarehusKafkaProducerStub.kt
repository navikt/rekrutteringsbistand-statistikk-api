package no.nav.statistikkapi.kafka

import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.logWithoutClassname
import no.nav.statistikkapi.stillinger.Stillingskategori

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori) {
        logWithoutClassname.info("Stubber sending av kandidatutfall til Datavarehus. Kandidatutfall: $kandidatutfall")
    }
}
