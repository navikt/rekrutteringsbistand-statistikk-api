package no.nav.rekrutteringsbistand.statistikk.datakatalog.hull

import no.nav.rekrutteringsbistand.statistikk.datakatalog.til
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class HullDatagrunnlag(
    utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>,
    utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>,
    dagensDato: () -> LocalDate
) {
    private val fraDatoHull = LocalDate.of(2021, 4, 8)
    private val gjeldendeDatoer = fraDatoHull til dagensDato()
    private val antallPresentertPerDagHarHull: Map<Pair<LocalDate, Boolean?>, Int> = finnAntallForHull(utfallElementPresentert, gjeldendeDatoer())
    private val antallFåttJobbPerDagHarHull: Map<Pair<LocalDate, Boolean?>, Int> = finnAntallForHull(utfallElementFåttJobben, gjeldendeDatoer())

    private fun finnAntallForHull(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) =
        datoer.flatMap { dag ->
            listOf(true, false, null).map { harHull ->
                (dag to harHull) to utfallselementer.filter { it.harHull == harHull && dag == it.tidspunkt.toLocalDate() }.count()
            }
        }.toMap()

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

    fun gjeldendeDatoer() = gjeldendeDatoer
}
