package no.nav.statistikkapi.kafka

import no.nav.rekrutteringsbistand.AvroKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

interface DatavarehusKafkaProducer {
    fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori)
}

class DatavarehusKafkaProducerImpl(private val producer: Producer<String, AvroKandidatutfall>) :
    DatavarehusKafkaProducer {

    companion object {
        const val topic = "toi.kandidatutfall"
    }

    override fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori) {

        val melding = AvroKandidatutfall(
            kandidatutfall.aktorId,
            kandidatutfall.utfall.name,
            kandidatutfall.navIdent,
            kandidatutfall.navKontor,
            kandidatutfall.kandidatlisteId.toString(),
            kandidatutfall.stillingsId.toString(),
            kandidatutfall.tidspunkt.toString(),
            stillingskategori.tilAvro()
        )
        val kafkaId = UUID.randomUUID().toString()
        producer.send(ProducerRecord(topic, kafkaId, melding)) { metadata, exception ->
            if (exception != null) {
                val msg =
                    "Forsøkte å sende melding til Datavarehus på topic [$topic]. Kanidatutfallets dbId: ${kandidatutfall.dbId}"
                log.error(msg, exception)
            } else {
                log.info("Sendte melding på Kafka-topic [$topic]. Kandidatutfallets dbId: ${kandidatutfall.dbId},kafkaId: $kafkaId, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
            }
        }
    }
}
