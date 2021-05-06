package statistikkapi.datakatalog.tilretteleggingsbehov

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class TilretteleggingsbehovDatagrunnlagTest {

    @Test
    fun `kun utfall med synlig kandidat skal være med i beregning av tilretteleggingsbehov`() {
        val måledato = LocalDate.of(2021, 5, 5)
        val utfallElementPresentert = listOf(
            KandidatutfallRepository.UtfallElement(true, 22, måledato.atStartOfDay(), listOf("arbeidstid"), true),
            KandidatutfallRepository.UtfallElement(null, 24, måledato.atStartOfDay(), emptyList(), false)
        )

        val datagrunnlag = TilretteleggingsbehovDatagrunnlag(utfallElementPresentert = utfallElementPresentert, utfallElementFåttJobben = listOf()) {
            LocalDate.of(
                2021,
                5,
                6
            )
        }

        val andelPresentertMedMinstEtTilretteleggingsbehovProsent = datagrunnlag.hentAndelPresentertMedMinstEttTilretteleggingsbehov(måledato) * 100
        assertThat(andelPresentertMedMinstEtTilretteleggingsbehovProsent).isEqualTo(100.toDouble())
    }

    @Test
    fun `test andel presentert med tilretteleggingsbehov av synlige kandidater`() {
        val måledato = LocalDate.of(2021, 5, 5)
        val utfallElementPresentert = listOf(
            KandidatutfallRepository.UtfallElement(true, 22, måledato.atStartOfDay(), listOf("arbeidstid"), true),
            KandidatutfallRepository.UtfallElement(null, 24, måledato.atStartOfDay(), emptyList(), true)
        )

        val datagrunnlag = TilretteleggingsbehovDatagrunnlag(utfallElementPresentert = utfallElementPresentert, utfallElementFåttJobben = listOf()) {
            LocalDate.of(
                2021,
                5,
                6
            )
        }

        val andelPresentertMedMinstEtTilretteleggingsbehovProsent = datagrunnlag.hentAndelPresentertMedMinstEttTilretteleggingsbehov(måledato) * 100
        assertThat(andelPresentertMedMinstEtTilretteleggingsbehovProsent).isEqualTo(50.toDouble())
    }
}
