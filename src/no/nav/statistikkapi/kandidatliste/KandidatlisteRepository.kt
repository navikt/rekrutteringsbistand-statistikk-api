package no.nav.statistikkapi.kandidatliste

import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun lagreKandidatliste(kandidatliste: OpprettKandidatliste) {

        dataSource.connection.use {
            it.prepareStatement(
                """insert into $kandidatlisteTabell (
                    $stillingsid,
                    $navident,
                    $kandidatlisteid,
                    $tidspunkt,
                    $er_direktemeldt,
                    $antall_stillinger,
                    $stilling_opprettet_tidspunkt
                    ) values (?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatliste.stillingsId)
                setString(2, kandidatliste.navIdent)
                setString(3, kandidatliste.kandidatlisteId)
                setTimestamp(4, Timestamp.valueOf(kandidatliste.tidspunktForHendelsen.toLocalDateTime()))
                setBoolean(5, kandidatliste.erDirektemeldt)
                setInt(6, kandidatliste.antallStillinger)
                setTimestamp(7, Timestamp.valueOf(kandidatliste.stillingOpprettetTidspunkt.toLocalDateTime()))
                executeUpdate()
            }
        }
    }

    fun kandidatlisteFinnesIDB(kandidatlisteId: String): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select 1 from $kandidatlisteTabell
                    where $kandidatlisteid = ?
                """.trimIndent()
            ). apply {
                setString(1, kandidatlisteId)
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    fun oppdaterKandidatliste(kandidatliste: OpprettKandidatliste) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update $kandidatlisteTabell
                    set $navident = ?
                        and $tidspunkt = ?
                        and $antall_stillinger = ?
                    where $stillingsid = ?
                        and $kandidatlisteid = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatliste.navIdent)
                setTimestamp(2, Timestamp.valueOf(kandidatliste.tidspunktForHendelsen.toLocalDateTime()))
                setInt(3, kandidatliste.antallStillinger)
                setString(4, kandidatliste.stillingsId)
                setString(5, kandidatliste.kandidatlisteId)
                executeUpdate()
            }
        }
    }

    companion object {
        const val dbId = "id"
        const val kandidatlisteTabell = "kandidatliste"
        const val navident = "navident"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val er_direktemeldt = "er_direktemeldt"
        const val antall_stillinger = "antall_stillinger"
        const val stilling_opprettet_tidspunkt = "stilling_opprettet_tidspunkt"
        const val tidspunkt = "tidspunkt"

        fun konverterTilKandidatliste(resultSet: ResultSet): Kandidatliste =
            Kandidatliste(
                dbId = resultSet.getLong(dbId),
                navIdent = resultSet.getString(navident),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteid)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsid)),
                erDirektemeldt = resultSet.getBoolean(er_direktemeldt),
                stillingOpprettetTidspunkt = resultSet.getTimestamp(stilling_opprettet_tidspunkt).toLocalDateTime(),
                antallStillinger = resultSet.getInt(antall_stillinger),
                tidspunkt = resultSet.getTimestamp(tidspunkt).toLocalDateTime()
            )
    }

}