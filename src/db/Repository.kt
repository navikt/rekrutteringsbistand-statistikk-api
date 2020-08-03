package no.nav.rekrutteringsbistand.statistikk.db

import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.IKKE_SENDT
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {

    companion object {
        const val kandidatutfallTabell = "kandidatutfall"
        const val aktørId = "aktorid"
        const val utfall = "utfall"
        const val navident = "navident"
        const val navkontor = "navkontor"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val tidspunkt = "tidspunkt"
        const val sendtStatus = "sendt_status"
        const val antallSendtForsøk = "antall_sendt_forsok"
        const val sisteSendtForsøk = "siste_sendt_forsok"

        fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
            Kandidatutfall(
                aktorId = resultSet.getString(aktørId),
                utfall = Utfall.valueOf(resultSet.getString(utfall)),
                navIdent = resultSet.getString(navident),
                navKontor = resultSet.getString(navkontor),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteid)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsid)),
                tidspunkt = resultSet.getTimestamp(tidspunkt).toLocalDateTime(),
                antallSendtForsøk = resultSet.getInt(antallSendtForsøk),
                sendtStatus = SendtStatus.valueOf(resultSet.getString(sendtStatus)),
                sisteSendtForsøk = resultSet.getTimestamp(sisteSendtForsøk)?.toLocalDateTime()
            )
    }

    fun lagreUtfall(kandidatutfall: OpprettKandidatutfall) {
        dataSource.connection.use {
            it.prepareStatement(
                """INSERT INTO $kandidatutfallTabell (
                                            $aktørId,
                                            $utfall,
                                            $navident,
                                            $navkontor,
                                            $kandidatlisteid,
                                            $stillingsid,
                                            $tidspunkt
                                        ) VALUES (?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatutfall.aktørId)
                setString(2, kandidatutfall.utfall)
                setString(3, kandidatutfall.navIdent)
                setString(4, kandidatutfall.navKontor)
                setString(5, kandidatutfall.kandidatlisteId)
                setString(6, kandidatutfall.stillingsId)
                setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
                executeUpdate()
            }
        }
    }

    fun connection(): Connection = dataSource.connection

    fun hentUsendteUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM $kandidatutfallTabell WHERE $sendtStatus = '${IKKE_SENDT.name}'")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }
}
