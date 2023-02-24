package no.nav.statistikkapi.kandidatliste

import no.nav.statistikkapi.atOslo
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun lagreKandidatlistehendelse(eventName: String, kandidatliste: OpprettKandidatliste) {
        dataSource.connection.use {
            it.prepareStatement(
                """insert into $kandidatlisteTabell (
                    $stillingsIdKolonne,
                    $kandidatlisteIdKolonne,
                    $erDirektemeldtKolonne,
                    $antallStillingerKolonne,
                    $antallKandidaterKolonne,
                    $stillingOpprettetTidspunktKolonne,
                    $stillingensPubliseringstidspunktKolonne,
                    $organisasjonsnummerKolonne,
                    $utførtAvNavIdentKolonne,
                    $tidspunktKolonne,
                    $eventNameKolonne
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatliste.stillingsId)
                setString(2, kandidatliste.kandidatlisteId)
                setBoolean(3, kandidatliste.erDirektemeldt)
                setInt(4, kandidatliste.antallStillinger)
                setInt(5, kandidatliste.antallKandidater)
                setTimestamp(6, Timestamp(kandidatliste.stillingOpprettetTidspunkt.toInstant().toEpochMilli()))
                setTimestamp(7, Timestamp(kandidatliste.stillingensPubliseringstidspunkt.toInstant().toEpochMilli()))
                setString(8, kandidatliste.organisasjonsnummer)
                setString(9, kandidatliste.utførtAvNavIdent)
                setTimestamp(10, Timestamp(kandidatliste.tidspunkt.toInstant().toEpochMilli()))
                setString(11, eventName)



                executeUpdate()
            }
        }
    }

    fun kandidatlisteFinnesIDB(kandidatlisteId: String): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select 1 from $kandidatlisteTabell
                    where $kandidatlisteIdKolonne = ?
                """.trimIndent()
            ). apply {
                setString(1, kandidatlisteId)
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    fun hentAntallKandidatlister(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteIdKolonne FROM $kandidatlisteTabell
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
                    SELECT DISTINCT $kandidatlisteIdKolonne FROM $kandidatlisteTabell
                        where ($erDirektemeldtKolonne = 'true')
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
                    SELECT DISTINCT $kandidatlisteIdKolonne FROM $kandidatlisteTabell
                        where ($erDirektemeldtKolonne = 'false')
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

        const val dbIdKolonne = "id"
        const val stillingsIdKolonne = "stillings_id"
        const val kandidatlisteIdKolonne = "kandidatliste_id"
        const val erDirektemeldtKolonne = "er_direktemeldt"
        const val antallStillingerKolonne = "antall_stillinger"
        const val antallKandidaterKolonne = "antall_kandidater"
        const val stillingOpprettetTidspunktKolonne = "stilling_opprettet_tidspunkt"
        const val stillingensPubliseringstidspunktKolonne = "stillingens_publiseringstidspunkt"
        const val organisasjonsnummerKolonne = "organisasjonsnummer"
        const val utførtAvNavIdentKolonne = "utført_av_nav_ident"
        const val tidspunktKolonne = "tidspunkt"
        const val eventNameKolonne = "event_name"

        // TODO: Brukes foreløpig kun i test-kode, hvis ikke blir tatt i bruk i prod-kode bør den flyttes til test-koden
        fun konverterTilKandidatliste(resultSet: ResultSet): Kandidatliste =
            Kandidatliste(
                dbId = resultSet.getLong(dbIdKolonne),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteIdKolonne)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsIdKolonne)),
                erDirektemeldt = resultSet.getBoolean(erDirektemeldtKolonne),
                antallStillinger = resultSet.getInt(antallStillingerKolonne),
                antallKandidater = resultSet.getInt(antallKandidaterKolonne),
                stillingOpprettetTidspunkt = resultSet.getTimestamp(stillingOpprettetTidspunktKolonne).toInstant().atOslo(),
                stillingensPubliseringstidspunkt = resultSet.getTimestamp(stillingensPubliseringstidspunktKolonne).toInstant().atOslo(),
                organisasjonsnummer = resultSet.getString(organisasjonsnummerKolonne),
                utførtAvNavIdent = resultSet.getString(utførtAvNavIdentKolonne),
                tidspunkt = resultSet.getTimestamp(tidspunktKolonne).toInstant().atOslo(),
                eventName = resultSet.getString(eventNameKolonne)
            )
    }
}