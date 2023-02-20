package no.nav.statistikkapi.kandidatlisteutfall

import no.nav.statistikkapi.kandidatutfall.SendtStatus
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class KandidatlisteutfallRepository(private val dataSource: DataSource) {

    fun lagreKandidatlisteutfall(kandidatlisteutfall: OpprettKandidatlisteutfall) {

        dataSource.connection.use {
            it.prepareStatement(
                """insert into $kandidatlisteutfallTabell (
                    $stillingsid,
                    $utfall,
                    $navident,
                    $kandidatlisteid,
                    $tidspunkt,
                    $er_direktemeldt,
                    $antall_stillinger,
                    $stilling_opprettet_tidspunkt
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatlisteutfall.stillingsId)
                setString(2, kandidatlisteutfall.utfall.name)
                setString(3, kandidatlisteutfall.navIdent)
                setString(4, kandidatlisteutfall.kandidatlisteId)
                setTimestamp(5, Timestamp.valueOf(kandidatlisteutfall.tidspunktForHendelsen.toLocalDateTime()))
                setBoolean(6, kandidatlisteutfall.erDirektemeldt)
                setInt(7, kandidatlisteutfall.antallStillinger)
                setTimestamp(8, Timestamp.valueOf(kandidatlisteutfall.stillingOpprettetTidspunkt.toLocalDateTime()))
                executeUpdate()
            }
        }
    }

    fun kandidatlisteutfallAlleredeLagret(kandidatlisteutfall: OpprettKandidatlisteutfall): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select 1 from $kandidatlisteutfallTabell
                    where $stillingsid = ?
                        and $kandidatlisteid = ?
                        and $utfall = ?
                        and $tidspunkt = ?
                        and $navident = ?
                """.trimIndent()
            ).apply {
                setString(1, kandidatlisteutfall.stillingsId)
                setString(2, kandidatlisteutfall.kandidatlisteId)
                setString(3, kandidatlisteutfall.utfall.name)
                setTimestamp(4, Timestamp.valueOf(kandidatlisteutfall.tidspunktForHendelsen.toLocalDateTime()))
                setString(5, kandidatlisteutfall.navIdent)
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    fun hentSisteKandidatlisteutfallForKandidatliste(kandidatlisteId: String): Kandidatlisteutfall? {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    select * from $kandidatlisteutfallTabell
                    where $kandidatlisteId = ?
                    order by $dbId desc limit 1
                """.trimIndent()
            ).apply {
                setString(1, kandidatlisteId)
                val resultSet = executeQuery()
                return if (resultSet.next()) konverterTilKandidatlisteutfall(resultSet)
                else null
            }
        }
    }

    fun hentSisteUtfallKandidatlisteForKandidatliste(kandidatlisteutfall: OpprettKandidatlisteutfall): UtfallKandidatliste? =
        hentSisteKandidatlisteutfallForKandidatliste(kandidatlisteutfall.kandidatlisteId)?.utfall

    fun registrerSendtForsøk(kandidatlisteutfall: Kandidatlisteutfall) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update $kandidatlisteutfallTabell
                    set $antallSendtForsøk = ?,
                        $sisteSendtForsøk = ?
                    where $dbId = ?
                """.trimIndent()
            ).apply {
                setInt(1, kandidatlisteutfall.antallSendtForsøk + 1)
                setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                setLong(3, kandidatlisteutfall.dbId)
                executeUpdate()
            }
        }
    }

    fun registrerSomSendt(kandidatlisteutfall: Kandidatlisteutfall) {
        dataSource.connection.use {
            it.prepareStatement(
                """
                    update $kandidatlisteutfallTabell
                    set $sendtStatus = ?
                    where $dbId = ?
                """.trimIndent()
            ).apply {
                setString(1, SendtStatus.SENDT.name)
                setLong(2, kandidatlisteutfall.dbId)
                executeUpdate()
            }
        }
    }

    fun hentUsendteKandidatlisteutfall(): List<Kandidatlisteutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement(
                    """
                        select * from $kandidatlisteutfallTabell
                        where $sendtStatus = '${SendtStatus.IKKE_SENDT.name}'
                        order by $dbId ASC
                    """.trimIndent()
                ).executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatlisteutfall(resultSet)
                else null
            }.toList()
        }
    }

    fun hentAlleKandidatlisteutfall(kandidatlisteId: String): List<Kandidatlisteutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("select * from $kandidatlisteutfallTabell where $kandidatlisteId = ? order by $dbId desc ")
                    .apply {
                        setString(1, kandidatlisteId)
                    }
                    .executeQuery()

            return generateSequence {
                if (resultSet.next()) konverterTilKandidatlisteutfall(resultSet)
                else null
            }.toList()
        }
    }

    companion object {
        const val dbId = "id"
        const val kandidatlisteutfallTabell = "kandidatlisteutfall"
        const val utfall = "utfall"
        const val navident = "navident"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val er_direktemeldt = "er_direktemeldt"
        const val antall_stillinger = "antall_stillinger"
        const val stilling_opprettet_tidspunkt = "stilling_opprettet_tidspunkt"
        const val tidspunkt = "tidspunkt"
        const val sendtStatus = "sendt_status"
        const val antallSendtForsøk = "antall_sendt_forsok"
        const val sisteSendtForsøk = "siste_sendt_forsok"

        fun konverterTilKandidatlisteutfall(resultSet: ResultSet): Kandidatlisteutfall =
            Kandidatlisteutfall(
                dbId = resultSet.getLong(dbId),
                utfall = UtfallKandidatliste.valueOf(resultSet.getString(utfall)),
                navIdent = resultSet.getString(navident),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteid)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsid)),
                erDirektemeldt = resultSet.getBoolean(er_direktemeldt),
                stillingOpprettetTidspunkt = resultSet.getTimestamp(stilling_opprettet_tidspunkt).toLocalDateTime(),
                antallStillinger = resultSet.getInt(antall_stillinger),
                tidspunkt = resultSet.getTimestamp(tidspunkt).toLocalDateTime(),
                antallSendtForsøk = resultSet.getInt(antallSendtForsøk),
                sendtStatus = SendtStatus.valueOf(resultSet.getString(sendtStatus)),
                sisteSendtForsøk = resultSet.getTimestamp(sisteSendtForsøk)?.toLocalDateTime()
            )
    }

}