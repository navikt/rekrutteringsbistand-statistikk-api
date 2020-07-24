package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log

class DatavarehusKafkaProducerStub: DatavarehusKafkaProducer {

    override fun send(kandidatutfall: OpprettKandidatutfall) {
        log.info("Stubber sending av statistikk på Kafka-topic. Utfall: ${kandidatutfall.utfall}")
    }
}
