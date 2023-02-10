package no.nav.statistikkapi.stillinger

import no.nav.rekrutteringsbistand.AvroStillingskategori

data class Stilling(
    val uuid: String,
    val stillingskategori: Stillingskategori
)

enum class Stillingskategori {
    STILLING, FORMIDLING, JOBBMESSE;

    fun tilAvro() = when (this) {
        STILLING -> AvroStillingskategori.STILLING
        FORMIDLING -> AvroStillingskategori.FORMIDLING
        JOBBMESSE -> AvroStillingskategori.JOBBMESSE
    }

    companion object {
        fun fraNavn(s: String?) = if (s == null) STILLING else valueOf(s)
    }
}
