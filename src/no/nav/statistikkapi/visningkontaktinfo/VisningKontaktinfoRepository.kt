package no.nav.statistikkapi.visningkontaktinfo

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

    fun hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("""
                with vist_kontaktinfo_per_kandidat_per_liste as (
                    select aktør_id, stilling_id
                    from visning_kontaktinfo
                    group by aktør_id, stilling_id
                ),
                kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo as (
                    select 1
                    from kandidatutfall
                    where aktorid in (select aktør_id from vist_kontaktinfo_per_kandidat_per_liste)
                    and (utfall = 'PRESENTERT' or utfall = 'FATT_JOBBEN')
                        and (
                            (alder < 30 or alder > 50) or 
                            (hull_i_cv is true) or 
                            (innsatsbehov in ('VARIG', 'BATT', 'BFORM'))
                        )
                    group by aktorid, stillingsid
                )
                select count(*) from kandidater_i_prioritert_målgruppe_med_åpnet_kontaktinfo;
            """.trimIndent()).executeQuery()

            return if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
        }
    }
}

