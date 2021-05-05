package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate

class AlderDatagrunnlag(
    utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>,
    utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>,
    dagensDato: () -> LocalDate
) {
    private val fraDatoAlder = LocalDate.of(2021, 4, 8)
    private val antallPresentertPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int> = finnAntallForAlder(utfallElementPresentert, gjeldendeDatoer(dagensDato))
    private val antallFåttJobbPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int> = finnAntallForAlder(utfallElementFåttJobben, gjeldendeDatoer(dagensDato))

    private fun finnAntallForAlder(utfallselementer: List<KandidatutfallRepository.UtfallElement>, datoer: List<LocalDate>) =
        datoer.flatMap { dag ->
            Aldersgruppe.values().map { aldersgruppe ->
                (dag to aldersgruppe) to utfallselementer.filter { dag == it.tidspunkt.toLocalDate() }
                    .mapNotNull { it.alder }.filter { aldersgruppe.inneholder(it) }
                    .count()
            }
        }.toMap()

    fun hentAntallPresentert(aldersgruppe: Aldersgruppe, dato: LocalDate) = antallPresentertPerDag[dato to aldersgruppe]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for presenterte $dato med aldersgruppe ${aldersgruppe}")

    fun hentAntallPresentertTotalt(dato: LocalDate) = Aldersgruppe.values().sumOf { hentAntallPresentert(it, dato) }

    fun hentAndelPresentertSenior(dato: LocalDate) =
        (hentAntallPresentert(Aldersgruppe.over50, dato).toDouble() / hentAntallPresentertTotalt(dato))
            .let { if (it.isNaN()) 0.0 else it }

    fun hentAndelPresentertUng(dato: LocalDate) =
        (hentAntallPresentert(Aldersgruppe.under30, dato).toDouble() / hentAntallPresentertTotalt(dato))
            .let { if (it.isNaN()) 0.0 else it }


    fun hentAntallFåttJobben(aldersgruppe: Aldersgruppe, dato: LocalDate) = antallFåttJobbPerDag[dato to aldersgruppe]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for fått jobben $dato med aldersgruppe ${aldersgruppe.toString()}")

    fun hentAntallFåttJobbenTotalt(dato: LocalDate) = Aldersgruppe.values().sumOf { hentAntallFåttJobben(it, dato) }

    fun hentAndelFåttJobbenSenior(dato: LocalDate) =
        (hentAntallFåttJobben(Aldersgruppe.over50, dato).toDouble() / hentAntallFåttJobbenTotalt(dato))
            .let { if (it.isNaN()) 0.0 else it }

    fun hentAndelFåttJobbenUng(dato: LocalDate) =
        (hentAntallFåttJobben(Aldersgruppe.under30, dato).toDouble() / hentAntallFåttJobbenTotalt(dato))
            .let { if (it.isNaN()) 0.0 else it }

    fun gjeldendeDatoer(dagensDato: () -> LocalDate) = fraDatoAlder til dagensDato()
}

enum class Aldersgruppe(val min: Int, val max: Int) {
    over50(50, 1000), under30(0, 29), mellom30og50(30, 49);

    fun inneholder(alder: Int): Boolean = alder in min..max
}

