package statistikkapi.datakatalog.tilretteleggingsbehov

import statistikkapi.datakatalog.til
import statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class TilretteleggingsbehovDatagrunnlag(
    utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>,
    utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>,
    dagensDato: () -> LocalDate
) {
    private val fraDatoTilrettelegingsbehov = LocalDate.of(2021, 5, 4)
    private val gjeldendeDatoer = fraDatoTilrettelegingsbehov til dagensDato()
    private val antallPresentertPerDagTilretteleggingsbehov: Map<LocalDate, ((List<String>) -> Boolean) -> Int> = finnAntallForTilretteleggingsbehov(utfallElementPresentert, gjeldendeDatoer())
    private val antallFåttJobbPerDagTilretteleggingsbehov: Map<LocalDate, ((List<String>) -> Boolean) -> Int> = finnAntallForTilretteleggingsbehov(utfallElementFåttJobben, gjeldendeDatoer())
    private val listeAvBehov = listOf(utfallElementPresentert,utfallElementFåttJobben).flatten().flatMap { it.tilretteleggingsbehov }.distinct()

    private fun finnAntallForTilretteleggingsbehov(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) :Map<LocalDate, ((List<String>) -> Boolean) -> Int> =
        datoer.associateWith { dag ->
            { tilretteleggingsbehovFilter ->
                utfallselementer
                    .filter { dag == it.tidspunkt.toLocalDate() }
                    .filter { it.synligKandidat }
                    .map { it.tilretteleggingsbehov }
                    .filter { tilretteleggingsbehovFilter(it) }.count()
            }
        }

    fun hentAntallPresentert(tilretteleggingsbehov: String, dato: LocalDate) =
        antallPresentertPerDagTilretteleggingsbehov[dato]!!(finnSpesifikt(tilretteleggingsbehov))

    fun hentAntallFåttJobben(tilretteleggingsbehov: String, dato: LocalDate) =
        antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(finnSpesifikt(tilretteleggingsbehov))

    fun hentAndelPresentertMedMinstEttTilretteleggingsbehov(dato: LocalDate) =
        (antallPresentertPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallPresentertPerDagTilretteleggingsbehov[dato]!!(totalAntall())).let { if (it.isNaN()) 0.0 else it }

    fun hentAndelFåttJobbenmedMinstEttTilretteleggingsbehov(dato: LocalDate) =
        (antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(minstEtTilretteleggingsbehov()).toDouble() /
                antallFåttJobbPerDagTilretteleggingsbehov[dato]!!(totalAntall())).let { if (it.isNaN()) 0.0 else it }

    fun listeAvBehov() = listeAvBehov

    fun gjeldendeDatoer() = gjeldendeDatoer
}

private fun finnSpesifikt(tilretteleggingsbehov: String): (List<String>) -> Boolean = { tilretteleggingsbehov in it }
private fun minstEtTilretteleggingsbehov(): (List<String>) -> Boolean = { it.isNotEmpty() }
private fun totalAntall(): (List<String>) -> Boolean = { true }
