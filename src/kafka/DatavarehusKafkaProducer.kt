package no.nav.rekrutteringsbistand.statistikk.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class DatavarehusKafkaProducer(bootstrapServers: String) {

    private val producer: KafkaProducer<String, String>

    init {
        val producerConfig: Properties = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        }
        producer = KafkaProducer(producerConfig)
    }

    companion object {
        const val TOPIC = "en-topic"
    }

    fun send(kandidatutfall: OpprettKandidatutfall) {
        // TODO: Bruk lagret ID og Avro
        producer.send(ProducerRecord(TOPIC, UUID.randomUUID().toString(), jacksonObjectMapper().writeValueAsString(kandidatutfall))) { _, _ ->
            log.info("Sendte melding")
        }
    }
}
