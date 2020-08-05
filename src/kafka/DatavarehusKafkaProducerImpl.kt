package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.AvroKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.*

interface DatavarehusKafkaProducer {
    fun send(kandidatutfall: Kandidatutfall)
}

class DatavarehusKafkaProducerImpl(config: Properties) : DatavarehusKafkaProducer {

    private val producer: KafkaProducer<String, AvroKandidatutfall> = KafkaProducer(config)

    companion object {
        const val TOPIC = "aapen-formidlingsutfallEndret-v1"
    }

    override fun send(kandidatutfall: Kandidatutfall) {
        val melding = AvroKandidatutfall(
            kandidatutfall.aktorId,
            kandidatutfall.utfall.name,
            kandidatutfall.navIdent,
            kandidatutfall.navKontor,
            kandidatutfall.kandidatlisteId.toString(),
            kandidatutfall.stillingsId.toString(),
            LocalDateTime.now().toString()
        )
        val kafkaId = UUID.randomUUID().toString()
        producer.send(ProducerRecord(TOPIC, kafkaId, melding)) { metadata, _ ->
            log.info("Sendte melding på Kafka. dbId: ${kandidatutfall.dbId}," +
                     "kafkaId: $kafkaId, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
        }
    }
}
