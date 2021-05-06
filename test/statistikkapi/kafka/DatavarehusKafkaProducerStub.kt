package statistikkapi.kafka

import statistikkapi.kandidatutfall.Kandidatutfall
import statistikkapi.log

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall) {
        log.info("Stubber sending av kandidatutfall til Datavarehus. Kandidatutfall: $kandidatutfall")
    }
}
