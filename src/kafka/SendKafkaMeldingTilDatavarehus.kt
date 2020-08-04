package no.nav.rekrutteringsbistand.statistikk.kafka

import io.micrometer.core.instrument.Metrics
import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.antallSendtForsøk
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.kandidatutfallTabell
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.sendtStatus
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.sisteSendtForsøk
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.SENDT
import no.nav.rekrutteringsbistand.statistikk.log
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime

fun sendKafkaMeldingTilDatavarehus(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
    val skalSendes = repository.hentUsendteUtfall()

    repository.connection().use { conn ->
        conn.autoCommit = false
        skalSendes.forEach { utfall ->
            // endre antallSendtForsøk og sisteSendtForsøk
            // commit

            try {

                kafkaProducer.send(utfall)

                // endre sendtStatus
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
                // commit
                conn.commit()

            } catch (e: Exception) {
                log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
//                conn.rollback()
                oppdaterFeiletForsøk(conn, utfall)
                return@forEach
            }
        }
    }
}

private fun oppdaterFeiletForsøk(conn: Connection, utfall: Kandidatutfall) {
    val antallForsøk = utfall.antallSendtForsøk + 1
    conn.prepareStatement(
        """UPDATE $kandidatutfallTabell
              SET $antallSendtForsøk = ?,
                  $sisteSendtForsøk = ?"""
    ).apply {
        setInt(1, antallForsøk)
        setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
        executeUpdate()
    }
    conn.commit()

    if (antallForsøk >= 5) {
        Metrics.counter(
            "rekrutteringsbistand.statistikk.kafka.feilet",
            "antallSendtForsøk", antallForsøk.toString()
        ).increment()
    }
}
