package no.nav.statistikkapi.kandidatliste

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class KandidatlisteRepository(private val dataSource: DataSource) {

    fun lagreKandidatlistehendelse(hendelse: Kandidatlistehendelse) {
        val stillingOpprettet = hendelse.stillingOpprettetTidspunkt?.let {
            Timestamp(hendelse.stillingOpprettetTidspunkt.toInstant().toEpochMilli())
        }

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
                setTimestamp(6, stillingOpprettet)
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

    fun hentAntallKandidatlisterForOpprettedeStillinger(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_kandidatlister.*) FROM (
                    SELECT DISTINCT $kandidatlisteIdKolonne FROM $kandidatlisteTabell
                    where $stillingOpprettetTidspunktKolonne is not null
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
                        where $erDirektemeldtKolonne is true
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

    fun harMottattOpprettetMelding(kandidatlisteId: UUID): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select 1 from ${kandidatlisteTabell} 
                where ${eventNameKolonne} = ?
                    and ${kandidatlisteIdKolonne} = ?
            """.trimIndent()
            ).apply {
                setString(1, opprettetKandidatlisteEventName)
                setString(2, kandidatlisteId.toString())
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }

    }
}
