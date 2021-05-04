package datakatalog

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DataGrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.til
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import org.junit.Test
import java.time.LocalDate


internal class DataGrunnlagTest {

    @Test
    fun `e`() {
        val presentertUtfall = listOf(
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 3).atTime(20, 59)),
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 3).atTime(20, 59)),
        )
        val fåttJobbenUtfall = listOf(
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 8).atTime(20, 59)),
            KandidatutfallRepository.UtfallElement(true, 29, LocalDate.of(2021, 3, 8).atTime(20, 59))
        )

        val datagrunnlag = DataGrunnlag(presentertUtfall, fåttJobbenUtfall)
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
