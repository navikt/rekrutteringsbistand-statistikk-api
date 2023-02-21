package no.nav.statistikkapi.kandidatliste

import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun opprettKandidatliste(kandidatliste: OpprettEllerOppdaterKandidatliste) {

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

    fun oppdaterKandidatliste(kandidatliste: OpprettEllerOppdaterKandidatliste) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update $kandidatlisteTabell
                    set $navident = ?,
                        $tidspunkt = ?,
                        $antall_stillinger = ?
                    where $kandidatlisteid = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatliste.navIdent)
                setTimestamp(2, Timestamp.valueOf(kandidatliste.tidspunktForHendelsen.toLocalDateTime()))
                setInt(3, kandidatliste.antallStillinger)
                setString(4, kandidatliste.kandidatlisteId)
                executeUpdate()
            }
        }
    }

    fun slettKandidatliste(kandidatlisteId: String) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    delete from $kandidatlisteTabell
                    where $kandidatlisteid = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatlisteId)
                executeUpdate()
            }
        }
    }

    fun hentAntallKandidatlister(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteid FROM $kandidatlisteTabell
                ) as unike_kandidatlister
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidatlister fra databasen")
            }
        }
    }

    fun hentAntallKandidatlisterTilknyttetDirektemeldteStillinger(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteid FROM $kandidatlisteTabell
                        where ($er_direktemeldt = 'true')
                ) as unike_kandidatlister
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidatlister tilknyttet direktemeldte stillinger fra databasen")
            }
        }
    }

    fun hentAntallKandidatlisterTilknyttetEksterneStillinger(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteid FROM $kandidatlisteTabell
                        where ($er_direktemeldt = 'false')
                ) as unike_kandidatlister
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidatlister tilknyttet eksterne stillinger fra databasen")
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

        // TODO: Brukes foreløpig kun i test-kode, hvis ikke blir tatt i bruk i prod-kode bør den flyttes til test-koden
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