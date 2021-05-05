package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.tilretteleggingsbehov.TilretteleggingsbehovDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class Datagrunnlag(private val utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>, private val utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>) {

    fun hentHullDatagrunnlag(datoer: List<LocalDate>) =
        HullDatagrunnlag(finnAntallPresentertPerDagPerHull(datoer), finnAntallFåttJobbPerDagHarHull(datoer))

    fun hentAlderDatagrunnlag(datoer: List<LocalDate>) =
        AlderDatagrunnlag(finnAntallPresentertPerDagPerAlder(datoer), finnAntallFåttJobbenPerDagPerAlder(datoer))

    fun hentTilretteleggingsbehovDatagrunnlag(datoer: List<LocalDate>) =
        TilretteleggingsbehovDatagrunnlag(finnAntallPresentertPerDagMedTilretteleggingsbehov(datoer), finnAntallFåttJobbenPerDagMedTilretteleggingsbehov(datoer), unikListeAvSamtligeTilretteleggingsbehov())

    private fun finnAntallPresentertPerDagPerAlder(datoer: List<LocalDate>) = finnAntallForAlder(utfallElementPresentert, datoer)

    private fun finnAntallFåttJobbenPerDagPerAlder(datoer: List<LocalDate>) = finnAntallForAlder(utfallElementFåttJobben, datoer)

    private fun finnAntallForAlder(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) =
        datoer.flatMap { dag ->
            Aldersgruppe.values().map { aldersgruppe ->
                (dag to aldersgruppe) to utfallselementer.filter { dag == it.tidspunkt.toLocalDate() }
                    .mapNotNull { it.alder }.filter { aldersgruppe.inneholder(it) }
                    .count()
            }
        }.toMap()

    private fun finnAntallPresentertPerDagPerHull(datoer: List<LocalDate>) = finnAntallForHull(utfallElementPresentert, datoer)

    private fun finnAntallFåttJobbPerDagHarHull(datoer: List<LocalDate>) = finnAntallForHull(utfallElementFåttJobben, datoer)

    private fun finnAntallForHull(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) =
        datoer.flatMap { dag ->
            listOf(true, false, null).map { harHull ->
                (dag to harHull) to utfallselementer.filter { it.harHull == harHull && dag == it.tidspunkt.toLocalDate() }.count()
            }
        }.toMap()

    private fun finnAntallPresentertPerDagMedTilretteleggingsbehov(datoer: List<LocalDate>) = finnAntallForTilretteleggingsbehov(utfallElementPresentert, datoer)

    private fun finnAntallFåttJobbenPerDagMedTilretteleggingsbehov(datoer: List<LocalDate>) = finnAntallForTilretteleggingsbehov(utfallElementFåttJobben, datoer)

    private fun finnAntallForTilretteleggingsbehov(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) :Map<LocalDate, ((List<String>) -> Boolean) -> Int> =
        datoer.associateWith { dag ->
            { tilretteleggingsbehovFilter ->
                utfallselementer.filter { dag == it.tidspunkt.toLocalDate() }
                    .map { it.tilretteleggingsbehov }
                    .filter { tilretteleggingsbehovFilter(it) }.count()
            }
        }

    private fun unikListeAvSamtligeTilretteleggingsbehov() =
        listOf(utfallElementPresentert,utfallElementFåttJobben).flatten().flatMap { it.tilretteleggingsbehov }.distinct()
}
