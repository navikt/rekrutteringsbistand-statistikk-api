package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.db.Repository

fun sendKafkaMeldingTilDatavarehus(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
    // Finn databserad
    // sende
    // skriv at er sendt til db
}
