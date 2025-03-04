package no.nav.statistikkapi.visningkontaktinfo

import no.nav.statistikkapi.kandidatutfall.Innsatsgruppe
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class VisningKontaktinfoRepository(private val dataSource: DataSource) {

    fun lagre(aktørId: String, stillingsId: UUID, tidspunkt: ZonedDateTime) {
        dataSource.connection.use {
            it.prepareStatement(
                """insert into visning_kontaktinfo (aktør_id, stilling_id, tidspunkt) values (?, ?, ?)"""
            ).apply {
                setString(1, aktørId)
                setObject(2, stillingsId)
                setTimestamp(3, Timestamp(tidspunkt.toInstant().toEpochMilli()))
                executeUpdate()
            }
        }
    }

    fun harAlleredeBlittLagret(aktørId: String, stillingsId: UUID, tidspunkt: ZonedDateTime): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select 1 from visning_kontaktinfo 
                where aktør_id = ?
                    and stilling_id = ?
                    and tidspunkt = ?
            """.trimIndent()
            ).apply {
                setString(1, aktørId)
                setObject(2, stillingsId)
                setTimestamp(3, Timestamp(tidspunkt.toInstant().toEpochMilli()))
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    private val ikkestandardInnsatsBehov = Innsatsgruppe.innsatsgrupperSomIkkeErStandardinnsats.joinToString { "'$it'" }

    fun hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                with vist_kontaktinfo_per_kandidat_per_liste as (
                    select aktør_id, stilling_id
                    from visning_kontaktinfo
                    group by aktør_id, stilling_id
                ),
                kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo as (
                    select 1
                    from kandidatutfall
                    inner join vist_kontaktinfo_per_kandidat_per_liste 
                        on kandidatutfall.aktorid = vist_kontaktinfo_per_kandidat_per_liste.aktør_id
                        and kandidatutfall.stillingsid = vist_kontaktinfo_per_kandidat_per_liste.stilling_id::text
                    where (utfall = 'PRESENTERT' or utfall = 'FATT_JOBBEN')
                        and (
                            (alder < 30 or alder > 49) or 
                            (hull_i_cv is true) or 
                            (innsatsbehov in ($ikkestandardInnsatsBehov))
                        )
                    group by aktorid, stillingsid
                )
                select count(*) from kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo;
            """.trimIndent()
            ).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    /**
     * Når man henter antallet må man kun hente de visningene av kontaktinfo som er på stillinger
     * der vi har lagret kandidatliste-informasjon i kandidatliste-tabellen. Dette skyldes at vi har data om
     * visning av kontaktinfo fra et mye tidligere tidspunkt enn for data om kandidatlister.
     */
    fun hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                select count(distinct stilling_id) from visning_kontaktinfo
                inner join kandidatliste
                        on kandidatliste.stillings_id = visning_kontaktinfo.stilling_id::text
                inner join kandidatutfall
                    on visning_kontaktinfo.stilling_id::text = kandidatutfall.stillingsid
                    and visning_kontaktinfo.aktør_id = kandidatutfall.aktorid
                where (
                    (alder < 30 or alder > 49) or
                    (hull_i_cv is true) or
                    (innsatsbehov in ($ikkestandardInnsatsBehov))
                );
            """.trimIndent()
            ).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }

    /**
     * Ser kun på stillinger opprettet fra 1.mars 2023 fordi det var da vi fikk inn data for opprettelse av nye stillinger
     */
    fun hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned(): Map<String, Int> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                select (
                           concat(
                                   (extract(year from visning_kontaktinfo.tidspunkt))::text,
                                   '-',
                                   (extract(month from visning_kontaktinfo.tidspunkt))::text
                               )
                           ) maaned, count(distinct stilling_id)
                from visning_kontaktinfo
                         inner join kandidatliste
                                    on kandidatliste.stillings_id = visning_kontaktinfo.stilling_id::text
                         inner join kandidatutfall
                                    on visning_kontaktinfo.stilling_id::text = kandidatutfall.stillingsid
                                        and visning_kontaktinfo.aktør_id = kandidatutfall.aktorid
                where kandidatliste.stilling_opprettet_tidspunkt >= '2023-03-01'
                  and (
                        (alder < 30 or alder > 49) or
                        (hull_i_cv is true) or
                        (innsatsbehov in ($ikkestandardInnsatsBehov))
                    )
                group by maaned;
            """.trimIndent()
            ).executeQuery()

            return generateSequence {
                if (resultSet.next()) {
                    val maaned = resultSet.getString(1)
                    val count = resultSet.getInt(2)
                    maaned to count
                } else null
            }.toMap()
        }
    }

    fun hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned(): Map<String, Int> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                select (
                           concat(
                                   (extract(year from visning_kontaktinfo.tidspunkt))::text,
                                   '-',
                                   (extract(month from visning_kontaktinfo.tidspunkt))::text
                               )
                           ) maaned, count(distinct stilling_id)
                from visning_kontaktinfo
                         inner join kandidatliste
                                    on kandidatliste.stillings_id = visning_kontaktinfo.stilling_id::text
                         inner join kandidatutfall
                                    on visning_kontaktinfo.stilling_id::text = kandidatutfall.stillingsid
                                        and visning_kontaktinfo.aktør_id = kandidatutfall.aktorid
                where kandidatliste.stilling_opprettet_tidspunkt >= '2023-03-01'
                group by maaned;
            """.trimIndent()
            ).executeQuery()

            return generateSequence {
                if (resultSet.next()) {
                    val maaned = resultSet.getString(1)
                    val count = resultSet.getInt(2)
                    maaned to count
                } else null
            }.toMap()
        }
    }
}

