package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import java.sql.Connection

fun DatabaseInterface.lagreUtfall(kandidatutfall: Kandidatutfall) {
    connection.use { connection ->
        connection.lagreUtfall(kandidatutfall)
        connection.commit()
    }
}

private fun Connection.lagreUtfall(kandidatutfall: Kandidatutfall) {
    this.prepareStatement("""
        INSERT INTO kandidatutfall(
            aktorid,
            utfall,
            navident,
            navkontor,
            kandidatlisteid,
            stillingsid
        )
        VALUES (?, ?, ?, ?, ?, ?)
    """).use {
        it.setString(1, kandidatutfall.aktorId)
        it.setString(2, kandidatutfall.utfall)
        it.setString(3, kandidatutfall.navIdent)
        it.setString(4, kandidatutfall.navIdent)
        it.setString(5, kandidatutfall.kandidatlisteId)
        it.setString(6, kandidatutfall.stillingsId)
        it.executeUpdate()
    }
}
