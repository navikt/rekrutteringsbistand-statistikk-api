package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.KandidatUtfall
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.*

interface DatavarehusKafkaProducer {
    fun send(kandidatutfall: OpprettKandidatutfall)
}

class DatavarehusKafkaProducerImpl(config: Properties): DatavarehusKafkaProducer {

    private val producer: KafkaProducer<String, KandidatUtfall> = KafkaProducer(config)

    companion object {
        const val TOPIC = "aapen-formidlingsutfallEndret-v1"
    }

    override fun send(kandidatutfall: OpprettKandidatutfall) {
        val melding = KandidatUtfall(
            kandidatutfall.aktørId,
            kandidatutfall.utfall,
            kandidatutfall.navIdent,
            kandidatutfall.navKontor,
            kandidatutfall.kandidatlisteId,
            kandidatutfall.stillingsId,
            LocalDateTime.now().toString()
        )
        producer.send(ProducerRecord(TOPIC, UUID.randomUUID().toString(), melding)) { _, _ ->
            log.info("Sendte melding")
        }
    }
}
