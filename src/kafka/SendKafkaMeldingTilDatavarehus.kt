package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.kandidatutfallTabell
import no.nav.rekrutteringsbistand.statistikk.log

fun sendKafkaMeldingTilDatavarehus(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
    // Finn databserad
    val skalSendes = repository.hentUsendteUtfall()

    repository.connection().use { conn ->
        // autocommit false
        conn.autoCommit = false

        try {
            skalSendes.forEach {
                // skriv at er sendt til db
//                conn.prepareStatement("UPDATE $kandidatutfallTabell SET $sendt")

                // sende
                kafkaProducer.send(it) // TODO type
                // commit
                conn.commit()
            }
        } catch (e: Exception) {
            // rollback
            log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
            conn.rollback()
        }


    }
}
