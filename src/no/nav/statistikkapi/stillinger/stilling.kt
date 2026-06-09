package no.nav.statistikkapi.stillinger

import no.nav.rekrutteringsbistand.AvroStillingskategori

data class Stilling(
    val uuid: String,
    val stillingskategori: Stillingskategori
)

enum class Stillingskategori {
    STILLING, FORMIDLING, JOBBMESSE, REKRUTTERINGSTREFF_FORMIDLING;

    fun tilAvro() = AvroStillingskategori.valueOf(name)

    companion object {
        fun fraNavn(s: String?) = if (s == null) STILLING else valueOf(s)
    }
}
