package no.nav.statistikkapi.tiltak

import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.log
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class TiltaksRepository(private val dataSource: DataSource) {

    companion object {
        const val tiltaksTabellLabel = "tiltak"
        const val avtaleIdLabel = "avtaleId"
        const val deltakerAktørIdLabel = "deltakerAktørId"
        const val deltakerFnrLabel = "deltakerFnr"
        const val enhetOppfolgingLabel = "enhetOppfolging"
        const val tiltakstypeLabel = "tiltakstype"
        const val avtaleInngåttLabel = "avtaleInngått"
        const val sistEndretLabel = "sistEndret"
    }

    data class OpprettTiltak(
        val avtaleId: UUID,
        val deltakerAktørId: String,
        val deltakerFnr: String,
        val enhetOppfolging: String,
        val tiltakstype: String,
        val avtaleInngått: ZonedDateTime,
        val sistEndret: ZonedDateTime
    )

    fun lagreTiltak(tiltak: OpprettTiltak) {

        val sistEndretIDb = sistEndretDatoForAvtaleId(tiltak)

        if(sistEndretIDb == null) {
            dataSource.connection.use {
                it.prepareStatement(
                    """INSERT INTO ${tiltaksTabellLabel} (
                               ${avtaleIdLabel},
                               ${deltakerAktørIdLabel},
                               ${deltakerFnrLabel},
                               ${enhetOppfolgingLabel},
                               ${tiltakstypeLabel},
                               ${avtaleInngåttLabel},
                               ${sistEndretLabel}
                ) VALUES (?, ?, ?, ?, ?, ?, ?)"""
                ).apply {
                    setString(1, tiltak.avtaleId.toString())
                    setString(2, tiltak.deltakerAktørId)
                    setString(3, tiltak.deltakerFnr)
                    setString(4, tiltak.enhetOppfolging)
                    setString(5, tiltak.tiltakstype)
                    setTimestamp(6, Timestamp(tiltak.avtaleInngått.toInstant().toEpochMilli()))
                    setTimestamp(7, Timestamp(tiltak.sistEndret.toInstant().toEpochMilli()))

                }.executeUpdate()
            }
        } else if(tiltak.sistEndret.isAfter(sistEndretIDb)) {
            dataSource.connection.use {
                it.prepareStatement(
                    """UPDATE ${tiltaksTabellLabel} 
                        SET ${deltakerAktørIdLabel} = ?,
                               ${deltakerFnrLabel} = ?,
                               ${enhetOppfolgingLabel} = ?,
                               ${tiltakstypeLabel} = ?,
                               ${avtaleInngåttLabel} = ?,
                               ${sistEndretLabel} = ?
                        WHERE ${avtaleIdLabel} = ?     
                               """
                ).apply {
                    setString(1, tiltak.deltakerAktørId)
                    setString(2, tiltak.deltakerFnr)
                    setString(3, tiltak.enhetOppfolging)
                    setString(4, tiltak.tiltakstype)
                    setTimestamp(5, Timestamp(tiltak.avtaleInngått.toInstant().toEpochMilli()))
                    setTimestamp(6, Timestamp(tiltak.sistEndret.toInstant().toEpochMilli()))
                    setString(7, tiltak.avtaleId.toString())

                }.executeUpdate()
            }
        } else {
            log.info("Ignorerer avtaleid ${tiltak.avtaleId} med sistEndret ${tiltak.sistEndret} fordi det finnes nyere oppdatering ${sistEndretIDb} i databasen")
        }


    }

    private fun sistEndretDatoForAvtaleId(tiltak: OpprettTiltak): ZonedDateTime? {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT ${sistEndretLabel} FROM ${tiltaksTabellLabel}
                  WHERE ${avtaleIdLabel} = ? 
                """.trimIndent()
            )
                .apply {
                    setString(1, tiltak.avtaleId.toString())
                }.executeQuery()

            if (resultSet.next() == false) return null

            return resultSet.getTimestamp(sistEndretLabel).toInstant().atOslo()
        }
    }
}