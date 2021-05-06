package statistikkapi.kafka

import no.nav.rekrutteringsbistand.AvroKandidatutfall
import statistikkapi.kandidatutfall.Kandidatutfall
import statistikkapi.log
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
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
            kandidatutfall.tidspunkt.toString()
        )
        val kafkaId = UUID.randomUUID().toString()
        producer.send(ProducerRecord(TOPIC, kafkaId, melding)) { metadata, _ ->
            log.info("Sendte melding på Kafka. dbId: ${kandidatutfall.dbId}," +
                     "kafkaId: $kafkaId, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
        }
    }
}
