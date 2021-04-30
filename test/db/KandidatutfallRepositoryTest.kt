package db

import assertk.assertThat
import assertk.assertions.isEqualTo
import etKandidatutfall
import etKontor1
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Utfall
import org.junit.After
import org.junit.Test
import java.time.LocalDate

class KandidatutfallRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
    }


    @Test
    fun `gitt en fått-jobben med ukjent hull men presentert med hull tell hentAntallFåttJobben som om cv har hull`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = true),
            LocalDate.of(2020, 1, 1).atTime(19, 54)
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = null),
            LocalDate.of(2020, 3, 4).atTime(20, 59)
        )

        val utfallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 5))

        val antallUtfallMedHull = utfallFåttJobben.count { it.harHull != null && it.harHull!! }
        val antallUtfallUtenHull = utfallFåttJobben.count { it.harHull != null && !it.harHull!! }
        val antallUtfallMedUkjentHull = utfallFåttJobben.count { it.harHull == null }

        assertThat(antallUtfallMedHull).isEqualTo(1)
        assertThat(antallUtfallUtenHull).isEqualTo(0)
        assertThat(antallUtfallMedUkjentHull).isEqualTo(0)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
