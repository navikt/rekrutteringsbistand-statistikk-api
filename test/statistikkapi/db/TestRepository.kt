package statistikkapi.db

import statistikkapi.kandidatutfall.Kandidatutfall
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.kandidatutfall.KandidatutfallRepository.Companion.konverterTilKandidatutfall
import javax.sql.DataSource

class TestRepository(private val dataSource: DataSource) {

    fun slettAlleUtfall() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${KandidatutfallRepository.kandidatutfallTabell}").execute()
        }
    }

    fun hentUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("SELECT * FROM ${KandidatutfallRepository.kandidatutfallTabell} ORDER BY id ASC").executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }
}
