package db

import assertk.assertThat
import assertk.assertions.isEqualTo
import etKandidatutfall
import etKontor1
import no.nav.rekrutteringsbistand.statistikk.datakatalog.Aldersgruppe
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DataGrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.til
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

        val antallUtfallMedHull = utfallFåttJobben.count { it.harHull == true }
        val antallUtfallUtenHull = utfallFåttJobben.count { it.harHull == false }
        val antallUtfallMedUkjentHull = utfallFåttJobben.count { it.harHull == null }

        assertThat(antallUtfallMedHull).isEqualTo(1)
        assertThat(antallUtfallUtenHull).isEqualTo(0)
        assertThat(antallUtfallMedUkjentHull).isEqualTo(0)
    }

    @Test
    fun `kan telle om cv har hull-status er ukjent på presenterte kandidater`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = null),
            LocalDate.of(2020, 3, 4).atTime(20, 59)
        )

        val antallUtfallMedUkjentHull =
            repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 5))
                .count { it.harHull == null }

        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
    }

    @Test
    fun `kan telle om cv har hull-status er ukjent på fått-jobben-kandidater`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = null),
            LocalDate.of(2020, 3, 4).atTime(20, 59)
        )

        val antallUtfallMedUkjentHull =
            repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 5))
                .count { it.harHull == null }

        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
    }

    @Test
    fun `e`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = true),
            LocalDate.of(2020, 3, 3).atTime(20, 59)
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = null),
            LocalDate.of(2020, 3, 8).atTime(20, 59)
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(aktørId = "annen", utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = true),
            LocalDate.of(2020, 3, 8).atTime(20, 59)
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(aktørId = "tredje", utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = true),
            LocalDate.of(2020, 3, 3).atTime(20, 59)
        )

        val datagrunnlag = DataGrunnlag(
            repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 10)),
            repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 10))
        ).hentHullDatagrunnlag(LocalDate.of(2020, 3,1) til LocalDate.of(2020,3,10))

        val tredjeMars = LocalDate.of(2020,3,3)
        val åttendeMars = LocalDate.of(2020,3,8)
        assertThat(datagrunnlag.hentAntallFåttJobben(true,tredjeMars)).isEqualTo(0)
        assertThat(datagrunnlag.hentAntallFåttJobben(null,tredjeMars)).isEqualTo(0)
        assertThat(datagrunnlag.hentAntallFåttJobben(true,åttendeMars)).isEqualTo(2)
        assertThat(datagrunnlag.hentAntallFåttJobben(null,åttendeMars)).isEqualTo(0)
        assertThat(datagrunnlag.hentAntallPresentert(true,tredjeMars)).isEqualTo(2)
        assertThat(datagrunnlag.hentAntallPresentert(null,tredjeMars)).isEqualTo(0)
        assertThat(datagrunnlag.hentAntallPresentert(true,åttendeMars)).isEqualTo(1)
        assertThat(datagrunnlag.hentAntallPresentert(null,åttendeMars)).isEqualTo(0)
    }


    @Test
    fun `gitt en fått-jobben med aldersgruppe 30-49 men presentert med aldersgruppe under 30 hentAntallFåttJobben som om aldersgruppe er under 30`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = true, alder = 29),
            LocalDate.of(2020, 1, 1).atTime(19, 29)
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = null, alder = 30),
            LocalDate.of(2020, 3, 4).atTime(20, 30)
        )

        val utfallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1), tilOgMed = LocalDate.of(2020, 3, 5))
            .mapNotNull { it.alder }

        val antallUtfallUnder30 = utfallFåttJobben.count { Aldersgruppe.under30.inneholder(it) }
        val antallUtfallOver50 = utfallFåttJobben.count { Aldersgruppe.over50.inneholder(it) }
        val antallUtfallMellom30Og50 = utfallFåttJobben.count { Aldersgruppe.mellom30og50.inneholder(it) }

        assertThat(antallUtfallUnder30).isEqualTo(1)
        assertThat(antallUtfallOver50).isEqualTo(0)
        assertThat(antallUtfallMellom30Og50).isEqualTo(0)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
