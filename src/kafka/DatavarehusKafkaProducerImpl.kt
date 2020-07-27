package no.nav.rekrutteringsbistand.statistikk.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

interface DatavarehusKafkaProducer {
    fun send(kandidatutfall: OpprettKandidatutfall)
}

class DatavarehusKafkaProducerImpl(config: Properties): DatavarehusKafkaProducer {

    private val producer: KafkaProducer<String, String> = KafkaProducer(config)

    companion object {
        const val TOPIC = "privat-formidlingsutfallEndret-v1"
    }

    override fun send(kandidatutfall: OpprettKandidatutfall) {
        producer.send(ProducerRecord(TOPIC, UUID.randomUUID().toString(), jacksonObjectMapper().writeValueAsString(kandidatutfall))) { _, _ ->
            log.info("Sendte melding")
        }
    }
}
