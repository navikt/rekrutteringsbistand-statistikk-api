package no.nav.statistikkapi.db

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import no.nav.statistikkapi.atOslo
import org.junit.After
import org.junit.Test
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.etKontor1
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class KandidatutfallRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
    }


    @Test
    fun `gitt en fått-jobben med ukjent hull men presentert med hull tell hentAntallFåttJobben som om cv har hull`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                navKontor = etKontor1,
                harHullICv = true,
                tidspunktForHendelsen = LocalDate.of(2020, 1, 1).atStartOfDay().atOslo()
            )
        )

        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                navKontor = etKontor1,
                harHullICv = null,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 4).atTime(20, 59).atOslo()
            )
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
    fun `gitt en presentert med kjent hull men senere med ukjent hull tell hentAntallPresentert som om cv har ukjent hull`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                navKontor = etKontor1,
                harHullICv = true,
                tidspunktForHendelsen = LocalDate.of(2020, 1, 1).atStartOfDay().atOslo()
            )
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                navKontor = etKontor1,
                harHullICv = null,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 4).atTime(20, 59).atOslo()
            )
        )

        val utfallFåttJobben = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1))

        val antallUtfallMedHull = utfallFåttJobben.count { it.harHull == true }
        val antallUtfallUtenHull = utfallFåttJobben.count { it.harHull == false }
        val antallUtfallMedUkjentHull = utfallFåttJobben.count { it.harHull == null }

        assertThat(antallUtfallMedHull).isEqualTo(0)
        assertThat(antallUtfallUtenHull).isEqualTo(0)
        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
    }

    @Test
    fun `kan telle om cv har hull-status er ukjent på presenterte kandidater`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                navKontor = etKontor1,
                harHullICv = null,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 4).atTime(20, 59).atOslo()
            )
        )

        val antallUtfallMedUkjentHull =
            repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).count { it.harHull == null }

        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
    }

    @Test
    fun `kan telle om cv har hull-status er ukjent på fått-jobben-kandidater`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                navKontor = etKontor1,
                harHullICv = null,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 4).atTime(20, 59).atOslo()
            )
        )

        val antallUtfallMedUkjentHull =
            repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1)).count { it.harHull == null }

        assertThat(antallUtfallMedUkjentHull).isEqualTo(1)
    }

    @Test
    fun `gitt en fått-jobben med aldersgruppe 30-49 men presentert med aldersgruppe under 30 hentAntallFåttJobben som om aldersgruppe er under 30`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                navKontor = etKontor1,
                harHullICv = true,
                alder = 29,
                tidspunktForHendelsen = LocalDate.of(2020, 1, 1).atTime(19, 29).atOslo()
            )
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                navKontor = etKontor1,
                harHullICv = null,
                alder = 30,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 4).atTime(20, 30).atOslo()
            )
        )

        val utfallFåttJobben =
            repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1)).mapNotNull { it.alder }

        val antallUtfallUnder30 = utfallFåttJobben.count { it < 30 }
        val antallUtfallOver50 = utfallFåttJobben.count { it > 50 }
        val antallUtfallMellom30g50 = utfallFåttJobben.count { it in 30..50 }

        assertThat(antallUtfallUnder30).isEqualTo(1)
        assertThat(antallUtfallOver50).isEqualTo(0)
        assertThat(antallUtfallMellom30g50).isEqualTo(0)
    }

    @Test
    fun `fått jobben skal ikke telles hvis det ikke er nyeste registrering`() {
        val fåttJobbenUtfall = etKandidatutfall.copy(
            utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = LocalDate.of(2020, 3, 1).atStartOfDay().atOslo()
        )
        val presentertUtfall = etKandidatutfall.copy(
            utfall = Utfall.PRESENTERT, tidspunktForHendelsen = LocalDate.of(2020, 3, 2).atStartOfDay().atOslo()
        )
        repository.lagreUtfall(fåttJobbenUtfall)
        repository.lagreUtfall(presentertUtfall)

        val antallFåttJobben = repository.hentUtfallFåttJobben(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `kan telle en kandidat som presentert`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                navKontor = etKontor1,
                harHullICv = true,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 59).atOslo()
            )
        )

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @Test
    fun `kan telle en kandidat som presentert selv om kandidaten senere fikk jobben`() {
        val presentertUtfall = etKandidatutfall.copy(
            utfall = Utfall.PRESENTERT, tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 59).atOslo()
        )
        val fåttJobbenUtfall = etKandidatutfall.copy(
            utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = LocalDate.of(2020, 3, 8).atTime(20, 59).atOslo()
        )
        repository.lagreUtfall(presentertUtfall)
        repository.lagreUtfall(fåttJobbenUtfall)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @Test
    fun `en kandidat som ble satt rett til fått jobben skal også telle som presentert`() {
        val fåttJobbenUtfall = etKandidatutfall.copy(
            utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = LocalDate.of(2020, 3, 8).atTime(20, 59).atOslo()
        )
        repository.lagreUtfall(fåttJobbenUtfall)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }


    @Test
    fun `skal kunne telle en kandidat som ble presentert og en annen kandidat som ble satt rett til fått jobben`() {
        val presentertUtfallKandidat1 = etKandidatutfall.copy(
            aktørId = "kandidat1",
            utfall = Utfall.PRESENTERT,
            tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 59).atOslo()
        )
        val fåttJobbenUtfallKandidat2 = etKandidatutfall.copy(
            aktørId = "kandidat2",
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = LocalDate.of(2020, 3, 9).atTime(20, 59).atOslo()
        )

        repository.lagreUtfall(presentertUtfallKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat2)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(2)
    }

    @Test
    fun `skal kunne telle en kandidat som ble presentert og senere fikk jobben og en annen kandidat som ble satt rett til fått jobben`() {
        val presentertUtfallKandidat1 = etKandidatutfall.copy(
            aktørId = "kandidat1",
            utfall = Utfall.PRESENTERT,
            tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 59).atOslo()
        )
        val fåttJobbenUtfallKandidat1 = etKandidatutfall.copy(
            aktørId = "kandidat1",
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = LocalDate.of(2020, 3, 8).atTime(20, 59).atOslo()
        )

        val fåttJobbenUtfallKandidat2 = etKandidatutfall.copy(
            aktørId = "kandidat2",
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = LocalDate.of(2020, 3, 9).atTime(20, 59).atOslo()
        )

        repository.lagreUtfall(presentertUtfallKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat1)
        repository.lagreUtfall(fåttJobbenUtfallKandidat2)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(2)
    }

    @Test
    fun `to presentert-utfall på samme kandidat og samme kandidatliste skal kun telles en gang`() {
        val førstePresentertUtfall = etKandidatutfall.copy(
            utfall = Utfall.PRESENTERT, tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 49).atOslo()
        )

        val andrePresentertUtfall = etKandidatutfall.copy(
            utfall = Utfall.PRESENTERT, tidspunktForHendelsen = LocalDate.of(2020, 3, 3).atTime(20, 59).atOslo()
        )

        repository.lagreUtfall(førstePresentertUtfall)
        repository.lagreUtfall(andrePresentertUtfall)

        val antallPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1)).size

        assertThat(antallPresentert).isEqualTo(1)
    }

    @Test
    fun `test lagring og uthenting av kandidat`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                tidspunktForHendelsen = LocalDate.of(2020, 3, 2).atTime(20, 49).atOslo()
            )
        )

        val utfallElementPresentert = repository.hentUtfallPresentert(fraOgMed = LocalDate.of(2020, 3, 1))

        assertThat(utfallElementPresentert.size).isEqualTo(1)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
