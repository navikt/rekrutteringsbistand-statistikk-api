package no.nav.rekrutteringsbistand.statistikk.datakatalog.tilretteleggingsbehov

import java.time.LocalDate

class TilretteleggingsbehovDatagrunnlag(
    private val antallPresentertPerDagTilretteleggingsbehov: Map<LocalDate, ((List<String>) -> Boolean) -> Int>,
    private val antallFåttJobbPerDagTilretteleggingsbehov: Map<LocalDate, ((List<String>) -> Boolean) -> Int>,
    private val listeAvBehov: List<String>
) {
    fun hentAntallPresentert(tilretteleggingsbehov: String, dato: LocalDate) =
        antallPresentertPerDagTilretteleggingsbehov[dato]!!(finnSpesifikt(tilretteleggingsbehov))

    fun hentAntallFåttJobben(tilretteleggingsbehov: String, dato: LocalDate) =
        antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(finnSpesifikt(tilretteleggingsbehov))

    fun hentAndelPresentert(dato: LocalDate) =
        antallPresentertPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallPresentertPerDagTilretteleggingsbehov[dato]!!(totalAntall())

    fun hentAndelFåttJobben(dato: LocalDate) =
        antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(totalAntall())

    fun listeAvBehov() = listeAvBehov
}

private fun finnSpesifikt(tilretteleggingsbehov: String): (List<String>) -> Boolean = { tilretteleggingsbehov in it }
private fun minstEtTilretteleggingsbehov(): (List<String>) -> Boolean = { it.isNotEmpty() }
private fun totalAntall(): (List<String>) -> Boolean = { true }

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
