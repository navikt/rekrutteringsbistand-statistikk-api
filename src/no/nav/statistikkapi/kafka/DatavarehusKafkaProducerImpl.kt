package no.nav.statistikkapi.kafka

import no.nav.rekrutteringsbistand.AvroKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import no.nav.statistikkapi.tiltak.TiltaksRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

interface DatavarehusKafkaProducer {
    fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori)
    fun send(tiltak: TiltaksRepository.Tiltak, stillingskategori: Stillingskategori)

}

class DatavarehusKafkaProducerImpl(private val producer: Producer<String, AvroKandidatutfall>): DatavarehusKafkaProducer {

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
                    "Forsøkte å sende utfall til Datavarehus på topic [$topic]. Kanidatutfallets dbId: ${kandidatutfall.dbId}"
                log.error(msg, exception)
            } else {
                log.info("Sendte utfall på Kafka-topic [$topic]. Kandidatutfallets dbId: ${kandidatutfall.dbId},kafkaId: $kafkaId, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
            }
        }
    }

    override fun send(tiltak: TiltaksRepository.Tiltak, stillingskategori: Stillingskategori) {

        val melding = AvroKandidatutfall(
            tiltak.deltakerAktørId,
            Utfall.FATT_JOBBEN.name,
            null,
            tiltak.enhetOppfolging,
            null,
            null,
            tiltak.avtaleInngått.toLocalDateTime().toString(),
            stillingskategori.tilAvro()
        )
        val kafkaId = UUID.randomUUID().toString()
        producer.send(ProducerRecord(topic, kafkaId, melding)) { metadata, exception ->
            if (exception != null) {
                val msg =
                    "Forsøkte å sende tiltak til Datavarehus på topic [$topic]. Tiltak avtaleid: ${tiltak.avtaleId}"
                log.error(msg, exception)
            } else {
                log.info("Sendte tiltak på Kafka-topic [$topic]. Tiltak dbId: ${tiltak.avtaleId},kafkaId: $kafkaId, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
            }
        }
    }
}



