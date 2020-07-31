package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: Kandidatutfall) {
        log.info("Stubber sending av statistikk på Kafka-topic. Utfall: ${kandidatutfall.utfall}")
    }
}
