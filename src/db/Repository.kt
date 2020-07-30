package no.nav.rekrutteringsbistand.statistikk.db

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.*
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime

class Repository(private val connection: Connection) {

    companion object {
        const val kandidatutfallTabell = "kandidatutfall"
        const val aktørId = "aktorid"
        const val utfall = "utfall"
        const val navident = "navident"
        const val navkontor = "navkontor"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val tidspunkt = "tidspunkt"
    }

    fun lagreUtfall(kandidatutfall: OpprettKandidatutfall) {
        connection.prepareStatement("""INSERT INTO $kandidatutfallTabell (
                                            $aktørId,
                                            $utfall,
                                            $navident,
                                            $navkontor,
                                            $kandidatlisteid,
                                            $stillingsid,
                                            $tidspunkt
                                        ) VALUES (?, ?, ?, ?, ?, ?, ?)"""
        ).use {
            it.setString(1, kandidatutfall.aktørId)
            it.setString(2, kandidatutfall.utfall)
            it.setString(3, kandidatutfall.navIdent)
            it.setString(4, kandidatutfall.navKontor)
            it.setString(5, kandidatutfall.kandidatlisteId)
            it.setString(6, kandidatutfall.stillingsId)
            it.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
            it.executeUpdate()
        }
    }

    fun hentUsendteUtfall() {
        // TODO
    }
}
