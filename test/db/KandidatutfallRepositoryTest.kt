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
import java.time.LocalDateTime

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
            LocalDate.of(2020, 1, 1).atStartOfDay()
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN, navKontor = etKontor1, harHullICv = null),
            LocalDate.of(2020, 3, 4).atTime(20, 59)
        )

        val utfallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1))

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
            repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1))
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
            repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1))
                .count { it.harHull == null }

        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
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

        val utfallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1))
            .mapNotNull { it.alder }

        val antallUtfallUnder30 = utfallFåttJobben.count { Aldersgruppe.under30.inneholder(it) }
        val antallUtfallOver50 = utfallFåttJobben.count { Aldersgruppe.over50.inneholder(it) }
        val antallUtfallMellom30Og50 = utfallFåttJobben.count { Aldersgruppe.mellom30og50.inneholder(it) }

        assertThat(antallUtfallUnder30).isEqualTo(1)
        assertThat(antallUtfallOver50).isEqualTo(0)
        assertThat(antallUtfallMellom30Og50).isEqualTo(0)
    }

    @Test
    fun `fått jobben skal ikke telles hvis det ikke er nyeste registrering`() {
        val fåttJobbenUtfall = etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN)
        val presentertUtfall = etKandidatutfall.copy(utfall = Utfall.PRESENTERT)
        repository.lagreUtfall(fåttJobbenUtfall, LocalDate.of(2020,3,1).atStartOfDay())
        repository.lagreUtfall(presentertUtfall, LocalDate.of(2020, 3, 2).atStartOfDay())

        val antallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `kan telle en kandidat som presentert`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT, navKontor = etKontor1, harHullICv = true),
            LocalDate.of(2020, 3, 3).atTime(20, 59)
        )

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @Test
    fun `kan telle en kandidat som presentert selv om kandidaten senere fikk jobben`() {
        val presentertUtfall = etKandidatutfall.copy(utfall = Utfall.PRESENTERT)
        val presentertTidspunkt = LocalDate.of(2020, 3, 3).atTime(20, 59)
        val fåttJobbenUtfall = etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN)
        val fåttJobbenTidspunkt = LocalDate.of(2020, 3, 8).atTime(20, 59)
        repository.lagreUtfall(presentertUtfall, presentertTidspunkt)
        repository.lagreUtfall(fåttJobbenUtfall, fåttJobbenTidspunkt)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @Test
    fun `en kandidat som ble satt rett til fått jobben skal også telle som presentert`() {
        val fåttJobbenUtfall = etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN)
        val fåttJobbenTidspunkt = LocalDate.of(2020, 3, 8).atTime(20, 59)
        repository.lagreUtfall(fåttJobbenUtfall, fåttJobbenTidspunkt)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }


    @Test
    fun `skal kunne telle en kandidat som ble presentert og en annen kandidat som ble satt rett til fått jobben`() {
        val presentertUtfallKandidat1 = etKandidatutfall.copy(aktørId = "kandidat1", utfall = Utfall.PRESENTERT)
        val presentertTidspunktKandidat1 = LocalDate.of(2020, 3, 3).atTime(20, 59)
        val fåttJobbenUtfallKandidat2 = etKandidatutfall.copy(aktørId = "kandidat2", utfall = Utfall.FATT_JOBBEN)
        val fåttJobbenTidspunktKandidat2 = LocalDate.of(2020, 3, 9).atTime(20, 59)
        repository.lagreUtfall(presentertUtfallKandidat1, presentertTidspunktKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat2, fåttJobbenTidspunktKandidat2)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(2)
    }

    @Test
    fun `skal kunne telle en kandidat som ble presentert og senere fikk jobben og en annen kandidat som ble satt rett til fått jobben`() {
        val presentertUtfallKandidat1 = etKandidatutfall.copy(aktørId = "kandidat1", utfall = Utfall.PRESENTERT)
        val presentertTidspunktKandidat1 = LocalDate.of(2020, 3, 3).atTime(20, 59)
        val fåttJobbenUtfallKandidat1 = etKandidatutfall.copy(aktørId = "kandidat1", utfall = Utfall.FATT_JOBBEN)
        val fåttJobbenTidspunktKandidat1 = LocalDate.of(2020, 3, 8).atTime(20, 59)
        val fåttJobbenUtfallKandidat2 = etKandidatutfall.copy(aktørId = "kandidat2", utfall = Utfall.FATT_JOBBEN)
        val fåttJobbenTidspunktKandidat2 = LocalDate.of(2020, 3, 9).atTime(20, 59)
        repository.lagreUtfall(presentertUtfallKandidat1, presentertTidspunktKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat1, fåttJobbenTidspunktKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat2, fåttJobbenTidspunktKandidat2)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(2)
    }

    @Test
    fun `to presentert-utfall på samme kandidat og samme kandidatliste skal kun telles en gang`() {
        val presentertUtfall = etKandidatutfall.copy(utfall = Utfall.PRESENTERT)
        val tidspunkt1 = LocalDate.of(2020, 3, 2).atTime(20, 49)
        val tidspunkt2 = LocalDate.of(2020, 3, 2).atTime(20, 59)
        repository.lagreUtfall(presentertUtfall, tidspunkt1)
        repository.lagreUtfall(presentertUtfall, tidspunkt2)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
