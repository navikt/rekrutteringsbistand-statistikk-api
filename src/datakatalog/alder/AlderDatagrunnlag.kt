package no.nav.rekrutteringsbistand.statistikk.datakatalog

import java.time.LocalDate

class AlderDatagrunnlag(
    private val antallPresentertPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int>,
    private val antallFåttJobbPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int>
) {
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
}

enum class Aldersgruppe(val min: Int, val max: Int) {
    over50(50, 1000), under30(0, 29), mellom30og50(30, 49);

    fun inneholder(alder: Int): Boolean = alder in min..max

    companion object {
        fun finnAldersgruppe(alder: Int) = when (alder) {
            in under30.min..under30.max -> under30
            in mellom30og50.min..mellom30og50.max -> mellom30og50
            else -> over50
        }
    }
}

