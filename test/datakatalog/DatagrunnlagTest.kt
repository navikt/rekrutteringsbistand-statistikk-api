package datakatalog

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.rekrutteringsbistand.statistikk.datakatalog.Datagrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.til
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import org.junit.Test
import java.time.LocalDate


internal class DatagrunnlagTest {

    @Test
    fun `integrasjonstest for målinger på presentert og fått jobben`() {
        val presentertUtfall = listOf(
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 3).atTime(20, 59), emptyList()),
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 3).atTime(20, 59), emptyList()),
        )
        val fåttJobbenUtfall = listOf(
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 8).atTime(20, 59), emptyList()),
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 8).atTime(20, 59), emptyList())
        )

        val datagrunnlag = Datagrunnlag(presentertUtfall, fåttJobbenUtfall)
        val hullDatagrunnlag = datagrunnlag.hentHullDatagrunnlag(LocalDate.of(2021, 3, 1) til LocalDate.of(2021, 3, 10))

        val tredjeMars = LocalDate.of(2021,3,3)
        val åttendeMars = LocalDate.of(2021,3,8)
        assertThat(hullDatagrunnlag.hentAntallFåttJobben(true,tredjeMars)).isEqualTo(0)
        assertThat(hullDatagrunnlag.hentAntallFåttJobben(null,tredjeMars)).isEqualTo(0)
        assertThat(hullDatagrunnlag.hentAntallFåttJobben(true,åttendeMars)).isEqualTo(2)
        assertThat(hullDatagrunnlag.hentAntallFåttJobben(null,åttendeMars)).isEqualTo(0)
        assertThat(hullDatagrunnlag.hentAntallPresentert(true,tredjeMars)).isEqualTo(2)
        assertThat(hullDatagrunnlag.hentAntallPresentert(null,tredjeMars)).isEqualTo(0)
        assertThat(hullDatagrunnlag.hentAntallPresentert(true,åttendeMars)).isEqualTo(0)
        assertThat(hullDatagrunnlag.hentAntallPresentert(null,åttendeMars)).isEqualTo(0)
    }
}
