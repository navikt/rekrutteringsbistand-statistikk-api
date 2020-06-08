package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime

fun DatabaseInterface.lagreUtfall(kandidatutfall: OpprettKandidatutfall) {
    connection.use {
        it.lagreUtfall(kandidatutfall)
        it.commit()
    }
}

private fun Connection.lagreUtfall(kandidatutfall: OpprettKandidatutfall) {
    prepareStatement("""
        INSERT INTO $kandidatutfallTabell (
            $aktørId,
            $utfall,
            $navident,
            $navkontor,
            $kandidatlisteid,
            $stillingsid,
            $tidspunkt
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
    """).use {
        it.setString(1, kandidatutfall.aktørId)
        it.setString(2, kandidatutfall.utfall)
        it.setString(3, kandidatutfall.navIdent)
        it.setString(4, kandidatutfall.navIdent)
        it.setString(5, kandidatutfall.kandidatlisteId)
        it.setString(6, kandidatutfall.stillingsId)
        it.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
        it.executeUpdate()
    }
}

const val kandidatutfallTabell = "kandidatutfall"
const val aktørId = "aktorid"
const val utfall = "utfall"
const val navident = "navident"
const val navkontor = "navkontor"
const val kandidatlisteid = "kandidatlisteid"
const val stillingsid = "stillingsid"
const val tidspunkt = "tidspunkt"
