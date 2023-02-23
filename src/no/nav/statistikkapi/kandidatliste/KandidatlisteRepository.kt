package no.nav.statistikkapi.kandidatliste

import io.ktor.server.util.*
import no.nav.statistikkapi.atOslo
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun opprettKandidatliste(kandidatliste: OpprettKandidatliste) {
        dataSource.connection.use {
            it.prepareStatement(
                """insert into $kandidatlisteTabell (
                    $stillingsId,
                    $navIdent,
                    $kandidatlisteId,
                    $tidspunkt,
                    $erDirektemeldt,
                    $antallStillinger,
                    $antallKandidater,
                    $stillingOpprettetTidspunkt,
                    $stillingensPubliseringstidspunkt
                    ) values (?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatliste.stillingsId)
                setString(2, kandidatliste.navIdent)
                setString(3, kandidatliste.kandidatlisteId)
                setTimestamp(4, Timestamp(kandidatliste.tidspunkt.toInstant().toEpochMilli()))
                setBoolean(5, kandidatliste.erDirektemeldt)
                setInt(6, kandidatliste.antallStillinger)
                setInt(7, kandidatliste.antallKandidater)
                setTimestamp(8, Timestamp(kandidatliste.stillingOpprettetTidspunkt.toInstant().toEpochMilli()))
                setTimestamp(9, Timestamp(kandidatliste.stillingensPubliseringstidspunkt.toInstant().toEpochMilli()))
                executeUpdate()
            }
        }
    }

    fun kandidatlisteFinnesIDB(kandidatlisteId: String): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select 1 from $kandidatlisteTabell
                    where $kandidatlisteId = ?
                """.trimIndent()
            ). apply {
                setString(1, kandidatlisteId)
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    fun oppdaterKandidatliste(kandidatliste: OppdaterKandidatliste) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update $kandidatlisteTabell
                    set $navIdent = ?,
                        $tidspunkt = ?,
                        $antallStillinger = ?
                    where $kandidatlisteId = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatliste.navIdent)
                setTimestamp(2, Timestamp.valueOf(kandidatliste.tidspunkt.toLocalDateTime()))
                setInt(3, kandidatliste.antallStillinger)
                setString(4, kandidatliste.kandidatlisteId)
                executeUpdate()
            }
        }
    }

    fun hentAntallKandidatlister(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteId FROM $kandidatlisteTabell
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
                    SELECT DISTINCT $kandidatlisteId FROM $kandidatlisteTabell
                        where ($erDirektemeldt = 'true')
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
                    SELECT DISTINCT $kandidatlisteId FROM $kandidatlisteTabell
                        where ($erDirektemeldt = 'false')
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
        const val kandidatlisteTabell = "kandidatliste"

        const val dbId = "id"
        const val stillingsId = "stillings_id"
        const val navIdent = "utført_av_nav_ident"
        const val kandidatlisteId = "kandidatliste_id"
        const val erDirektemeldt = "er_direktemeldt"
        const val antallStillinger = "antall_stillinger"
        const val antallKandidater = "antall_kandidater"
        const val stillingOpprettetTidspunkt = "stilling_opprettet_tidspunkt"
        const val stillingensPubliseringstidspunkt = "stillingens_publiseringstidspunkt"
        const val organisasjonsnummer = "organisasjonsnummer"
        const val tidspunkt = "tidspunkt"

        // TODO: Brukes foreløpig kun i test-kode, hvis ikke blir tatt i bruk i prod-kode bør den flyttes til test-koden
        fun konverterTilKandidatliste(resultSet: ResultSet): Kandidatliste =
            Kandidatliste(
                dbId = resultSet.getLong(dbId),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteId)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsId)),
                erDirektemeldt = resultSet.getBoolean(erDirektemeldt),
                antallStillinger = resultSet.getInt(antallStillinger),
                antallKandidater = resultSet.getInt(antallKandidater),
                stillingOpprettetTidspunkt = resultSet.getTimestamp(stillingOpprettetTidspunkt).toInstant().atOslo(),
                stillingensPubliseringstidspunkt = resultSet.getTimestamp(stillingensPubliseringstidspunkt).toInstant().atOslo(),
                organisasjonsnummer = resultSet.getString(organisasjonsnummer),
                tidspunkt = resultSet.getTimestamp(tidspunkt).toInstant().atOslo(),
            )
    }
}