package db

import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
import java.sql.Connection
import java.sql.ResultSet

class TestRepository(private val connection: Connection) {

    fun slettAlleUtfall() {
        connection.prepareStatement("DELETE FROM ${Repository.kandidatutfallTabell}").execute()
    }

    fun hentUtfall(): List<Kandidatutfall> {
        val resultSet = connection.prepareStatement("SELECT * FROM ${Repository.kandidatutfallTabell}").executeQuery()
        return generateSequence {
            if (resultSet.next()) konverterTilKandidatutfall(resultSet)
            else null
        }.toList()
    }

    private fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
        Kandidatutfall(
            aktorId = resultSet.getString(Repository.aktørId),
            utfall = resultSet.getString(Repository.utfall),
            navIdent = resultSet.getString(Repository.navident),
            navKontor = resultSet.getString(Repository.navkontor),
            kandidatlisteId = resultSet.getString(Repository.kandidatlisteid),
            stillingsId = resultSet.getString(Repository.stillingsid),
            tidspunkt = resultSet.getTimestamp(Repository.tidspunkt).toLocalDateTime()
        )

}
