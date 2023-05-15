package no.nav.statistikkapi.kandidatliste

import java.sql.SQLException
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

/**
 * Det funker å telle antall kandidatlister uten å se på stillingskategori fordi alle radene i kandidatliste-tabellen
 * gjelder kandidatlister tilknyttet stillinger med stillingskategori STILLING eller null.
 */
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

    fun hentAntallKandidatlisterTilknyttetStillingPerMåned(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                select count(unike_kandidatlister.*), (
                    concat(
                        (extract(year from unike_kandidatlister.stilling_opprettet_tidspunkt))::text,
                        '-',
                        (extract(month from unike_kandidatlister.stilling_opprettet_tidspunkt))::text
                    )
                ) maaned
                from (
                    select distinct kandidatliste_id, stilling_opprettet_tidspunkt from kandidatliste
                    where stilling_opprettet_tidspunkt is not null
                ) as unike_kandidatlister
                where (
                    concat(
                        (extract(year from unike_kandidatlister.stilling_opprettet_tidspunkt))::text,
                        '-',
                        (extract(month from unike_kandidatlister.stilling_opprettet_tidspunkt))::text
                    )
                ) >= '2023-3'
                group by maaned
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidatlister per måned fra databasen")
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

    fun hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                with id_siste_utfall_per_kandidat_per_liste as (
                    select max(id) from kandidatutfall 
                    group by aktorid, kandidatlisteid
                ),
                presentert_utfall as (
                    select * from kandidatutfall 
                    where id in (select * from id_siste_utfall_per_kandidat_per_liste)
                    and (utfall = 'FATT_JOBBEN' or utfall = 'PRESENTERT')
                )
                select count(distinct $kandidatlisteIdKolonne)
                from $kandidatlisteTabell
                         inner join presentert_utfall
                                    on presentert_utfall.kandidatlisteid = $kandidatlisteTabell.$kandidatlisteIdKolonne
                         inner join stilling
                                    on $kandidatlisteTabell.$stillingsIdKolonne = stilling.uuid
                where $kandidatlisteTabell.$erDirektemeldtKolonne is true 
                    and $kandidatlisteTabell.$stillingOpprettetTidspunktKolonne is not null
                    and stilling.stillingskategori = 'STILLING' or stilling.stillingskategori is null;
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    fun hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                with id_siste_utfall_per_kandidat_per_liste as (
                    select max(id) from kandidatutfall 
                    group by aktorid, kandidatlisteid
                ),
                fått_jobben_utfall as (
                    select * from kandidatutfall 
                    where id in (select * from id_siste_utfall_per_kandidat_per_liste)
                    and utfall = 'FATT_JOBBEN'
                )
                select count(distinct $kandidatlisteIdKolonne)
                from $kandidatlisteTabell
                         inner join fått_jobben_utfall
                            on fått_jobben_utfall.kandidatlisteid = $kandidatlisteTabell.$kandidatlisteIdKolonne
                         inner join stilling
                            on $kandidatlisteTabell.$stillingsIdKolonne = stilling.uuid
                where $kandidatlisteTabell.$stillingOpprettetTidspunktKolonne is not null
                    and (stilling.stillingskategori = 'STILLING' or stilling.stillingskategori is null)
                    and (
                        (fått_jobben_utfall.alder < 30 or fått_jobben_utfall.alder > 49) or 
                        (fått_jobben_utfall.hull_i_cv is true) or 
                        (fått_jobben_utfall.innsatsbehov in ('VARIG', 'BATT', 'BFORM'))
                    )
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    fun hentAntallDirektemeldteStillingerSomHarTomKandidatliste(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                    with id_siste_hendelse_per_kandidatliste as (
                        select max(id) from $kandidatlisteTabell
                        group by $kandidatlisteIdKolonne
                    )
                    select count(distinct $kandidatlisteIdKolonne) from $kandidatlisteTabell
                    where id in (select * from id_siste_hendelse_per_kandidatliste)
                    and $antallKandidaterKolonne = 0
                    and $stillingOpprettetTidspunktKolonne is not null;
                """.trimIndent()
            ).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    fun hentAntallUnikeArbeidsgivereForDirektemeldteStillinger(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("""
                select count(distinct $organisasjonsnummerKolonne) 
                from $kandidatlisteTabell
                where $erDirektemeldtKolonne is true 
                and $stillingOpprettetTidspunktKolonne is not null;
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    fun hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                with id_siste_utfall_per_kandidat_per_liste as (
                    select max(id) from kandidatutfall 
                    group by aktorid, kandidatlisteid
                ),
                fått_jobben_utfall as (
                    select * from kandidatutfall 
                    where id in (select * from id_siste_utfall_per_kandidat_per_liste)
                    and utfall = 'FATT_JOBBEN'
                )
                select count(distinct $kandidatlisteIdKolonne)
                from $kandidatlisteTabell
                         inner join fått_jobben_utfall
                            on fått_jobben_utfall.kandidatlisteid = $kandidatlisteTabell.$kandidatlisteIdKolonne
                         inner join stilling
                            on $kandidatlisteTabell.$stillingsIdKolonne = stilling.uuid
                where $kandidatlisteTabell.$stillingOpprettetTidspunktKolonne is not null
                    and $erDirektemeldtKolonne is true
                    and (stilling.stillingskategori = 'STILLING' or stilling.stillingskategori is null)
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }
}
