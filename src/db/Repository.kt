package no.nav.rekrutteringsbistand.statistikk.db

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import java.sql.Timestamp
import java.time.LocalDateTime
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

    fun connection() = dataSource.connection

    fun hentUsendteUtfall(): List<Kandidatutfall> {
        // TODO
        return listOf<Kandidatutfall>()
    }
}
