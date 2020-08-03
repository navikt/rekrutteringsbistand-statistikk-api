package no.nav.rekrutteringsbistand.statistikk.kafka

import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.antallSendtForsøk
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.kandidatutfallTabell
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.sendtStatus
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.sisteSendtForsøk
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.SENDT
import no.nav.rekrutteringsbistand.statistikk.log
import java.sql.Timestamp
import java.time.LocalDateTime

fun sendKafkaMeldingTilDatavarehus(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
    val skalSendes = repository.hentUsendteUtfall()

    repository.connection().use { conn ->
        conn.autoCommit = false
        try {
            skalSendes.forEach { utfall ->
                conn.prepareStatement(
                    """UPDATE $kandidatutfallTabell
                          SET $sendtStatus = ?,
                              $antallSendtForsøk = ?,
                              $sisteSendtForsøk = ?"""
                ).apply {
                    setString(1, SENDT.name)
                    setInt(2, utfall.antallSendtForsøk + 1)
                    setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                    executeUpdate()
                }
                kafkaProducer.send(utfall)
                conn.commit()
            }
        } catch (e: Exception) {
            log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
            conn.rollback()
        }
    }
}
