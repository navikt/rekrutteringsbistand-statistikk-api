package no.nav.rekrutteringsbistand.statistikk.datakatalog

import java.time.LocalDate

class AlderDatagrunnlag(
    private val antallPresentertPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int>,
    private val antallFåttJobbPerDag: Map<Pair<LocalDate, Aldersgruppe>, Int>
) {
    fun hentAntallPresentert(aldersgruppe: Aldersgruppe, dato: LocalDate) = antallPresentertPerDag[dato to aldersgruppe]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for presenterte $dato med aldersgruppe ${aldersgruppe.toString()}")

    fun hentAntallFåttJobben(aldersgruppe: Aldersgruppe, dato: LocalDate) = antallFåttJobbPerDag[dato to aldersgruppe]
        ?: throw RuntimeException("datagrunnlag eksisterer ikke for fått jobben $dato med aldersgruppe ${aldersgruppe.toString()}")
}

enum class Aldersgruppe(val min: Int, val max: Int) {
    over50(50,1000), under30(0, 29), mellom30og50(30, 49)
}

