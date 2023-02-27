package no.nav.statistikkapi.kandidatliste

import no.nav.statistikkapi.atOslo
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun lagreKandidatlistehendelse(hendelse: Kandidatlistehendelse) {
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
                    $tidspunktForHendelsenKolonne,
                    $eventNameKolonne
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, hendelse.stillingsId)
                setString(2, hendelse.kandidatlisteId)
                setBoolean(3, hendelse.erDirektemeldt)
                setInt(4, hendelse.antallStillinger)
                setInt(5, hendelse.antallKandidater)
                setTimestamp(6, Timestamp(hendelse.stillingOpprettetTidspunkt.toInstant().toEpochMilli()))
                setTimestamp(7, Timestamp(hendelse.stillingensPubliseringstidspunkt.toInstant().toEpochMilli()))
                setString(8, hendelse.organisasjonsnummer)
                setString(9, hendelse.utførtAvNavIdent)
                setTimestamp(10, Timestamp(hendelse.tidspunkt.toInstant().toEpochMilli()))
                setString(11, hendelse.eventName)
                executeUpdate()
            }
        }
    }

    fun kandidatlisteFinnesIDb(kandidatlisteId: UUID): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select 1 from $kandidatlisteTabell
                    where $kandidatlisteIdKolonne = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatlisteId.toString())
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
        const val tidspunktForHendelsenKolonne = "tidspunkt_for_hendelsen"
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
                stillingOpprettetTidspunkt = resultSet.getTimestamp(stillingOpprettetTidspunktKolonne).toInstant()
                    .atOslo(),
                stillingensPubliseringstidspunkt = resultSet.getTimestamp(stillingensPubliseringstidspunktKolonne)
                    .toInstant().atOslo(),
                organisasjonsnummer = resultSet.getString(organisasjonsnummerKolonne),
                utførtAvNavIdent = resultSet.getString(utførtAvNavIdentKolonne),
                tidspunkt = resultSet.getTimestamp(tidspunktForHendelsenKolonne).toInstant().atOslo(),
                eventName = resultSet.getString(eventNameKolonne)
            )
    }

    fun hendelseFinnesFraFør(
        eventName: String,
        kandidatlisteId: UUID,
        tidspunktForHendelsen: ZonedDateTime
    ): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select 1 from ${kandidatlisteTabell} 
                where ${eventNameKolonne} = ?
                    and ${kandidatlisteIdKolonne} = ?
                    and ${tidspunktForHendelsenKolonne} = ?
            """.trimIndent()
            ).apply {
                setString(1, eventName)
                setString(2, kandidatlisteId.toString())
                setTimestamp(3, Timestamp(tidspunktForHendelsen.toInstant().toEpochMilli()))
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }

    }
}

// TODO Slettes eller flyttes til testkode, fordi den ikke brukes av prodkode i skrivende stund.
data class Kandidatliste(
    val dbId: Long,
    val kandidatlisteId: UUID,
    val stillingsId: UUID,
    val erDirektemeldt: Boolean,
    val stillingOpprettetTidspunkt: ZonedDateTime,
    val stillingensPubliseringstidspunkt: ZonedDateTime,
    val organisasjonsnummer: String,
    val antallStillinger: Int,
    val antallKandidater: Int,
    val tidspunkt: ZonedDateTime,
    val utførtAvNavIdent: String,
    val eventName: String
)
