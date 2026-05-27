package no.nav.statistikkapi.stillinger

data class Stilling(
    val uuid: String,
    val stillingskategori: Stillingskategori
)

enum class Stillingskategori {
    STILLING, FORMIDLING, JOBBMESSE, REKRUTTERINGSTREFF_FORMIDLING;

    fun tilAvro() = name

    companion object {
        fun fraNavn(s: String?) = if (s == null) STILLING else valueOf(s)
    }
}
