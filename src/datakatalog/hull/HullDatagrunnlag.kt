package no.nav.rekrutteringsbistand.statistikk.datakatalog.hull

import java.time.LocalDate

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
