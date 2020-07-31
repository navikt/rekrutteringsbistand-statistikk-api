package db

import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus
import no.nav.rekrutteringsbistand.statistikk.db.Utfall
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

class TestRepository(private val dataSource: DataSource) {

    fun slettAlleUtfall() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${Repository.kandidatutfallTabell}").execute()
        }
    }

    fun hentUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("SELECT * FROM ${Repository.kandidatutfallTabell}").executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }

    private fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
        Kandidatutfall(
            aktorId = resultSet.getString(Repository.aktørId),
            utfall = Utfall.valueOf(resultSet.getString(Repository.utfall)),
            navIdent = resultSet.getString(Repository.navident),
            navKontor = resultSet.getString(Repository.navkontor),
            kandidatlisteId = UUID.fromString(resultSet.getString(Repository.kandidatlisteid)),
            stillingsId = UUID.fromString(resultSet.getString(Repository.stillingsid)),
            tidspunkt = resultSet.getTimestamp(Repository.tidspunkt).toLocalDateTime(),
            antallSendtForsøk = resultSet.getInt(Repository.antallSendtForsøk),
            sendtStatus = SendtStatus.valueOf(resultSet.getString(Repository.sendtStatus)),
            sisteSendtForsøk = resultSet.getTimestamp(Repository.sisteSendtForsøk)?.toLocalDateTime()
        )

}
