package no.nav.statistikkapi.kandidatliste

import java.sql.SQLException
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
                        where $erDirektemeldtKolonne is true and $stillingOpprettetTidspunktKolonne is not null
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
                        where $erDirektemeldtKolonne is false and $stillingOpprettetTidspunktKolonne is not null
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

    fun hentAntallStillingerForEksterneStillingsannonserMedKandidatliste(): Int {
        dataSource.connection.use {
            try {
                val resultSet = it.prepareStatement(
                    """
                with id_og_siste_tidspunkt_for_direktemeldte_stillinger as (
                        select first_value($tidspunktForHendelsenKolonne) 
                            over (partition by $kandidatlisteIdKolonne order by $tidspunktForHendelsenKolonne desc) 
                            as tidspunkt, $kandidatlisteIdKolonne
                        from $kandidatlisteTabell
                        where $erDirektemeldtKolonne is false and $stillingOpprettetTidspunktKolonne is not null
                        group by $kandidatlisteIdKolonne, $tidspunktForHendelsenKolonne
                    )
                    
                    select distinct $kandidatlisteTabell.$kandidatlisteIdKolonne, $kandidatlisteTabell.$antallStillingerKolonne from $kandidatlisteTabell
                    join id_og_siste_tidspunkt_for_direktemeldte_stillinger
                        on id_og_siste_tidspunkt_for_direktemeldte_stillinger.kandidatliste_id = $kandidatlisteTabell.$kandidatlisteIdKolonne
                        and id_og_siste_tidspunkt_for_direktemeldte_stillinger.tidspunkt = $kandidatlisteTabell.$tidspunktForHendelsenKolonne
            """.trimIndent()
                ).executeQuery()

                var antallStillinger = 0

                while (resultSet.next()) {
                    antallStillinger += resultSet.getInt(antallStillingerKolonne)
                }
                return antallStillinger

            } catch (e: SQLException) {
                throw RuntimeException("Prøvde å hente antall stillinger tilknyttet eksterne stillingsannonser fra databasen")
            }
        }
    }

    fun hentAntallStillingerForDirektemeldteStillingsannonser(): Int {
        dataSource.connection.use {
            try {
                val resultSet = it.prepareStatement(
                    """
                    with id_og_siste_tidspunkt_for_direktemeldte_stillinger as (
                        select first_value($tidspunktForHendelsenKolonne) 
                            over (partition by $kandidatlisteIdKolonne order by $tidspunktForHendelsenKolonne desc) 
                            as tidspunkt, $kandidatlisteIdKolonne
                        from $kandidatlisteTabell
                        where $erDirektemeldtKolonne is true and $stillingOpprettetTidspunktKolonne is not null
                        group by $kandidatlisteIdKolonne, $tidspunktForHendelsenKolonne
                    )
                    
                    select distinct $kandidatlisteTabell.$kandidatlisteIdKolonne, $kandidatlisteTabell.$antallStillingerKolonne from $kandidatlisteTabell
                    join id_og_siste_tidspunkt_for_direktemeldte_stillinger
                        on id_og_siste_tidspunkt_for_direktemeldte_stillinger.kandidatliste_id = $kandidatlisteTabell.$kandidatlisteIdKolonne
                        and id_og_siste_tidspunkt_for_direktemeldte_stillinger.tidspunkt = $kandidatlisteTabell.$tidspunktForHendelsenKolonne
            """.trimIndent()
                ).executeQuery()

                var antallStillinger = 0

                while (resultSet.next()) {
                    antallStillinger += resultSet.getInt(antallStillingerKolonne)
                }
                return antallStillinger

            } catch (e: SQLException) {
                throw RuntimeException("Prøvde å hente antall stillinger tilknyttet direktemeldte stillingsannonser fra databasen")
            }
        }
    }

    fun hentAntallStillingerForStillingsannonserMedKandidatliste(): Int {
        dataSource.connection.use {
            try {
                val resultSet = it.prepareStatement(
                    """
                with id_og_siste_tidspunkt_for_direktemeldte_stillinger as (
                        select first_value($tidspunktForHendelsenKolonne) 
                            over (partition by $kandidatlisteIdKolonne order by $tidspunktForHendelsenKolonne desc) 
                            as tidspunkt, $kandidatlisteIdKolonne
                        from $kandidatlisteTabell
                        where $stillingOpprettetTidspunktKolonne is not null
                        group by $kandidatlisteIdKolonne, $tidspunktForHendelsenKolonne
                    )
                    
                    select distinct $kandidatlisteTabell.$kandidatlisteIdKolonne, $kandidatlisteTabell.$antallStillingerKolonne from $kandidatlisteTabell
                    join id_og_siste_tidspunkt_for_direktemeldte_stillinger
                        on id_og_siste_tidspunkt_for_direktemeldte_stillinger.kandidatliste_id = $kandidatlisteTabell.$kandidatlisteIdKolonne
                        and id_og_siste_tidspunkt_for_direktemeldte_stillinger.tidspunkt = $kandidatlisteTabell.$tidspunktForHendelsenKolonne
            """.trimIndent()
                ).executeQuery()

                var antallStillinger = 0

                while (resultSet.next()) {
                    antallStillinger += resultSet.getInt(antallStillingerKolonne)
                }
                return antallStillinger

            } catch (e: SQLException) {
                throw RuntimeException("Prøvde å hente antall stillinger tilknyttet stillingsannonser med kandidatliste fra databasen")
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
    /*
    with vist_kontaktinfo_per_kandidat_per_liste as (
                    select aktør_id, stilling_id
                    from visning_kontaktinfo
                    group by aktør_id, stilling_id
                ),
                kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo as (
                    select 1
                    from kandidatutfall
                    where aktorid in (select aktør_id from vist_kontaktinfo_per_kandidat_per_liste)
                    and stillingsid in (select stilling_id::text from vist_kontaktinfo_per_kandidat_per_liste)
                    and (utfall = 'PRESENTERT' or utfall = 'FATT_JOBBEN')
                        and (
                            (alder < 30 or alder > 50) or
                            (hull_i_cv is true) or
                            (innsatsbehov in ('VARIG', 'BATT', 'BFORM'))
                        )
                    group by aktorid, stillingsid
                )
                select count(*) from kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo;
    */

    fun hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("""
                with unike_kandidatlister_tilknyttet_direktemeldte_stillinger as (
                    select distinct $kandidatlisteIdKolonne FROM $kandidatlisteTabell
                    where $stillingOpprettetTidspunktKolonne is not null and $erDirektemeldtKolonne is true
                ),
                    kandidatlister_med_minst_en_presentert_kandidat as (
                        select aktorid, utfall, kandidatlisteid from kandidatutfall
                        where kandidatlisteid in (select $kandidatlisteIdKolonne from unike_kandidatlister_tilknyttet_direktemeldte_stillinger)
                        and (utfall = 'PRESENTERT' or utfall = 'FATT_JOBBEN')
                        group by aktorid, utfall, kandidatlisteid
                    ),
                    unike_kandidatlister_med_minst_en_presentert_kandidat as (
                        select distinct kandidatlisteid from kandidatlister_med_minst_en_presentert_kandidat
                    )

                select count(*) from unike_kandidatlister_med_minst_en_presentert_kandidat
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }
}
