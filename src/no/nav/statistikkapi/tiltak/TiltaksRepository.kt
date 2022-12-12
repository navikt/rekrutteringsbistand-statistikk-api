package no.nav.statistikkapi.tiltak

import no.nav.statistikkapi.HentStatistikk
import java.sql.Date
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
        if (avtaleIdFinnes(tiltak)) return

        dataSource.connection.use {
            it.prepareStatement(
                """INSERT INTO ${tiltaksTabellLabel} (
                               ${avtaleIdLabel},
                               ${deltakerAktørIdLabel},
                               ${deltakerFnrLabel},
                               ${enhetOppfolgingLabel},
                               ${tiltakstypeLabel},
                               ${avtaleInngåttLabel}
                ) VALUES (?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, tiltak.avtaleId.toString())
                setString(2, tiltak.deltakerAktørId)
                setString(3, tiltak.deltakerFnr)
                setString(4, tiltak.enhetOppfolging)
                setString(5, tiltak.tiltakstype)
                setTimestamp(6, Timestamp(tiltak.avtaleInngått.toInstant().toEpochMilli()))
            }.executeUpdate()
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

    fun avtaleIdFinnes(tiltak: OpprettTiltak): Boolean {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT 1 FROM ${tiltaksTabellLabel}
                  WHERE ${avtaleIdLabel} = ? 
                """.trimIndent()
            )
                .apply {
                    setString(1, tiltak.avtaleId.toString())
                }.executeQuery()

            return resultSet.next()
        }
    }
}