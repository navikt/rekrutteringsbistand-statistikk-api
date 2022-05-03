package no.nav.statistikkapi.db

import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository.Companion.konverterTilKandidatutfall
import no.nav.statistikkapi.stillinger.StillingRepository
import javax.sql.DataSource

class TestRepository(private val dataSource: DataSource) {

    fun slettAlleUtfall() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${KandidatutfallRepository.kandidatutfallTabell}").execute()
        }
    }

    fun slettAlleLønnstilskudd() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${KandidatutfallRepository.lønnstilskuddTabell}").execute()
        }
    }

    fun hentUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM ${KandidatutfallRepository.kandidatutfallTabell} ORDER BY id ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }

    fun hentAntallStillinger() = dataSource.connection.use {
        it.prepareStatement("SELECT count(*) FROM ${StillingRepository.stillingtabell}").executeQuery().run {
            next()
            getInt(1)
        }
    }

    fun slettAlleStillinger() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${StillingRepository.stillingtabell}").execute()
        }
    }

}
