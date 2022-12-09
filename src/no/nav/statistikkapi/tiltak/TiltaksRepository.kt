package no.nav.statistikkapi.tiltak

import no.nav.statistikkapi.HentStatistikk
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class TiltaksRepository(private val dataSource: DataSource) {

    companion object {
        const val tiltaksTabell = "tiltak"
        const val avtaleId = "avtaleid"
        const val tiltakstype = "tiltakstype"
        const val aktørId = "aktorid"
        const val fnr = "fnr"
        const val navkontor = "navkontor"
        const val avtaleInngått = "avtaleInngått"
    }
    data class OpprettTiltak (
        val avtaleId: UUID,
        val aktørId: String,
        val fnr: String,
        val navkontor: String,
        val tiltakstype: String,
        val avtaleInngått: LocalDateTime
    )

    fun lagreTiltak(tiltak: OpprettTiltak) {
        dataSource.connection.use {
            it.prepareStatement(
                """INSERT INTO ${tiltaksTabell} (
                               ${avtaleId},
                               ${aktørId},
                               ${fnr},
                               ${navkontor},
                               ${tiltakstype},
                               ${avtaleInngått}
                ) VALUES (?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, tiltak.avtaleId.toString())
                setString(2, tiltak.aktørId)
                setString(3, tiltak.fnr)
                setString(4, tiltak.navkontor)
                setString(5, tiltak.tiltakstype)
                setTimestamp(6, Timestamp.valueOf(tiltak.avtaleInngått))
            }.executeUpdate()
        }
    }

    fun hentAktøridFåttJobbenTiltak(hentStatistikk: HentStatistikk): List<Tiltakstilfelle> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT ${aktørId}, ${tiltakstype} FROM ${tiltaksTabell}
                  WHERE ${navkontor} = ? 
                  AND ${avtaleInngått} BETWEEN ? AND ? 
                """.trimIndent()
            )
                .apply {
                    setString(1, hentStatistikk.navKontor)
                    setDate(2, Date.valueOf(hentStatistikk.fraOgMed))
                    setDate(3, Date.valueOf(hentStatistikk.tilOgMed))

                }.executeQuery()

            return generateSequence {
                if (!resultSet.next()) null
                else Tiltakstilfelle(resultSet.getString(aktørId), resultSet.getString(tiltakstype).tilTiltakstype())
            }.toList()
        }
    }
}