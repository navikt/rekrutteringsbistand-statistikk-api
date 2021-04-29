package db

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository.Companion.konverterTilKandidatutfall
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
