package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.db.Database

fun sendKafkaMeldingTilDatavarehus(db: Database, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
    // Finn databserad
    // sende
    // skriv at er sendt til db
}
