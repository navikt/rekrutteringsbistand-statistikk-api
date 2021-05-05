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
        (antallPresentertPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallPresentertPerDagTilretteleggingsbehov[dato]!!(totalAntall())).let { if (it.isNaN()) 0.0 else it }

    fun hentAndelFåttJobben(dato: LocalDate) =
        (antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(totalAntall())).let { if (it.isNaN()) 0.0 else it }

    fun listeAvBehov() = listeAvBehov
}

private fun finnSpesifikt(tilretteleggingsbehov: String): (List<String>) -> Boolean = { tilretteleggingsbehov in it }
private fun minstEtTilretteleggingsbehov(): (List<String>) -> Boolean = { it.isNotEmpty() }
private fun totalAntall(): (List<String>) -> Boolean = { true }
