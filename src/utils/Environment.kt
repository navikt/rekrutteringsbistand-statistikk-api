package no.nav.rekrutteringsbistand.statistikk.utils

data class Environment(
    val profil: String = System.getenv("PROFIL") ?: "lokal"
)
