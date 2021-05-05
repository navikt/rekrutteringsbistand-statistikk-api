package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.datakatalog.tilretteleggingsbehov.TilretteleggingsbehovDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class Datagrunnlag(val utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>, val utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>) {



    fun hentTilretteleggingsbehovDatagrunnlag(datoer: List<LocalDate>) =
        TilretteleggingsbehovDatagrunnlag(finnAntallPresentertPerDagMedTilretteleggingsbehov(datoer), finnAntallFåttJobbenPerDagMedTilretteleggingsbehov(datoer), unikListeAvSamtligeTilretteleggingsbehov())



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
