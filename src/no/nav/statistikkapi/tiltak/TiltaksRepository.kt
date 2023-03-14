package no.nav.statistikkapi.tiltak

import no.nav.statistikkapi.HentStatistikk
import no.nav.statistikkapi.SendtStatus
import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
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
        const val sendtStatusLabel = "sendt_status"
        const val antallSendtForsøkLabel = "antall_sendt_forsok"
        const val sisteSendtForsøkLabel = "siste_sendt_forsok"
        const val dbIdLabel = "id"

        fun konverterTilTiltak(resultSet: ResultSet): Tiltak =
            Tiltak(
                deltakerAktørId = resultSet.getString(TiltaksRepository.deltakerAktørIdLabel),
                avtaleId = UUID.fromString(resultSet.getString(TiltaksRepository.avtaleIdLabel)),
                deltakerFnr = resultSet.getString(deltakerFnrLabel),
                enhetOppfolging = resultSet.getString(enhetOppfolgingLabel),
                tiltakstype = resultSet.getString(tiltakstypeLabel),
                avtaleInngått = resultSet.getTimestamp(TiltaksRepository.avtaleInngåttLabel).toInstant().atOslo(),
                sistEndret = resultSet.getTimestamp(TiltaksRepository.sistEndretLabel).toInstant().atOslo(),
                antallSendtForsøk = resultSet.getInt(TiltaksRepository.antallSendtForsøkLabel),
                sendtStatus = SendtStatus.valueOf(resultSet.getString(TiltaksRepository.sendtStatusLabel)),
                sisteSendtForsøk = resultSet.getTimestamp(TiltaksRepository.sisteSendtForsøkLabel)?.toLocalDateTime(),
            )
    }

    data class Tiltak(
        val avtaleId: UUID,
        val deltakerAktørId: String,
        val deltakerFnr: String,
        val enhetOppfolging: String,
        val tiltakstype: String,
        val avtaleInngått: ZonedDateTime,
        val sistEndret: ZonedDateTime,
        val sendtStatus: SendtStatus,
        val antallSendtForsøk: Int,
        val sisteSendtForsøk: LocalDateTime?,
    )

    fun lagreTiltak(tiltak: Tiltak) {

        val sistEndretIDb = sistEndretDatoForAvtaleId(tiltak)

        if (sistEndretIDb == null) {
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
        } else if (tiltak.sistEndret.isAfter(sistEndretIDb)) {
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

    fun hentAktøridFåttJobbenTiltak(hentStatistikk: HentStatistikk): List<Tiltakstilfelle> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT ${deltakerAktørIdLabel}, ${tiltakstypeLabel} FROM ${tiltaksTabellLabel}
                  WHERE ${enhetOppfolgingLabel} = ? 
                  AND ${avtaleInngåttLabel} BETWEEN ? AND ? 
                """.trimIndent()
            )
                .apply {
                    setString(1, hentStatistikk.navKontor)
                    setDate(2, Date.valueOf(hentStatistikk.fraOgMed))
                    setDate(3, Date.valueOf(hentStatistikk.tilOgMed))

                }.executeQuery()

            return generateSequence {
                if (!resultSet.next()) null
                else {
                    Tiltakstilfelle(
                        resultSet.getString(deltakerAktørIdLabel),
                        resultSet.getString(tiltakstypeLabel).tilTiltakstype()
                    )
                }
            }.toList()
        }
    }

    fun sistEndretDatoForAvtaleId(tiltak: Tiltak): ZonedDateTime? {
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

    fun hentUsendteTiltak(): List<Tiltak> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM ${TiltaksRepository.tiltaksTabellLabel} WHERE ${TiltaksRepository.sendtStatusLabel} = '${SendtStatus.IKKE_SENDT.name}' ORDER BY ${TiltaksRepository.dbIdLabel} ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) TiltaksRepository.konverterTilTiltak(resultSet)
                else null
            }.toList()
        }
    }

    fun registrerSendtForsøk(tiltak: Tiltak) {
        dataSource.connection.use {
            it.prepareStatement(
                """UPDATE ${TiltaksRepository.tiltaksTabellLabel}
                      SET ${TiltaksRepository.antallSendtForsøkLabel} = ?,
                          ${TiltaksRepository.sisteSendtForsøkLabel} = ?
                    WHERE ${TiltaksRepository.avtaleIdLabel} = ?"""
            ).apply {
                setInt(1, tiltak.antallSendtForsøk + 1)
                setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                setString(3, tiltak.avtaleId.toString())
                executeUpdate()
            }
        }

    }

    fun registrerSomSendt(tiltak: Tiltak) {
        dataSource.connection.use {
            it.prepareStatement(
                """UPDATE ${TiltaksRepository.tiltaksTabellLabel}
                      SET ${TiltaksRepository.sendtStatusLabel} = ?
                    WHERE ${TiltaksRepository.avtaleIdLabel} = ?"""
            ).apply {
                setString(1, SendtStatus.SENDT.name)
                setString(2, tiltak.avtaleId.toString())
                executeUpdate()
            }
        }
    }
}