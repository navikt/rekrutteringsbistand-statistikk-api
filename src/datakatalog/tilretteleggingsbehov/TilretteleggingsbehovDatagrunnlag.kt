package no.nav.rekrutteringsbistand.statistikk.datakatalog.tilretteleggingsbehov

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class TilretteleggingsbehovDatagrunnlag(
    private val antallPresentertPerDagTilretteleggingsbehov: Map<Pair<LocalDate, String>, Int>,
    private val antallFåttJobbPerDagTilretteleggingsbehov: Map<Pair<LocalDate, String>, Int>,
    private val utfallElementPresentert: Map<LocalDate, List<KandidatutfallRepository.UtfallElement>>,
    private val utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>
) {
    fun hentAntallPresentert(tilretteleggingsbehov: String, dato: LocalDate) = antallPresentertPerDagTilretteleggingsbehov[dato to tilretteleggingsbehov]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for presenterte $dato med tilrettelggingsbehov $tilretteleggingsbehov")

    fun hentAntallFåttJobben(tilretteleggingsbehov: String, dato: LocalDate) = antallFåttJobbPerDagTilretteleggingsbehov[dato to tilretteleggingsbehov]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for fått jobben $dato med tilrettelggingsbehov $tilretteleggingsbehov")

    fun hentAndelPresentertMedEtEllerFlereTilrettelggingsbehov(dato: LocalDate) {
        val antallPresentertUtenTilretteleggingsbehov = utfallElementPresentert[dato].count { it.tilretteleggingsbehov !=  }
        val antallPresentertMedTilretteleggingsbehov = antallPresentertUtenTilretteleggingsbehov.
    }

    fun hentAndelFåttJobben(dato: LocalDate) = hentAntallFåttJobben(true, dato).let { antallMedHull ->
        antallMedHull.toDouble() / (antallMedHull + hentAntallFåttJobben(false, dato))
    }.let { if (it.isNaN()) 0.0 else it }
}

/*

class HullDatagrunnlag(
    private val antallPresentertPerDagHarHull: Map<Pair<LocalDate, Boolean?>, Int>,
    private val antallFåttJobbPerDagHarHull: Map<Pair<LocalDate, Boolean?>, Int>
) {
    fun hentAntallPresentert(harHull: Boolean?, dato: LocalDate) = antallPresentertPerDagHarHull[dato to harHull]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for presenterte $dato med harHull $harHull")
    fun hentAntallFåttJobben(harHull: Boolean?, dato: LocalDate) = antallFåttJobbPerDagHarHull[dato to harHull]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for fått jobben $dato med harHull $harHull")

    fun hentAndelPresentert(dato: LocalDate) = hentAntallPresentert(true, dato).let { antallMedHull ->
        antallMedHull.toDouble() / (antallMedHull + hentAntallPresentert(false, dato))
    }.let { if (it.isNaN()) 0.0 else it }
    fun hentAndelFåttJobben(dato: LocalDate) = hentAntallFåttJobben(true, dato).let { antallMedHull ->
        antallMedHull.toDouble() / (antallMedHull + hentAntallFåttJobben(false, dato))
    }.let { if (it.isNaN()) 0.0 else it }
}

 */
