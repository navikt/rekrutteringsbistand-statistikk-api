package no.nav.rekrutteringsbistand.statistikk.utils

private const val clusterEnvVar = "NAIS_CLUSTER_NAME"

enum class Miljø {
    LOKALT, DEV_FSS, PROD_FSS
}

data class Environment(
    val miljø: Miljø = when (System.getenv(clusterEnvVar)) {
        "dev-fss" -> Miljø.DEV_FSS
        "prod-fss" -> Miljø.PROD_FSS
        else -> Miljø.LOKALT
    }
) {
    fun isLokal() = miljø == Miljø.LOKALT
    fun isDev() = miljø == Miljø.DEV_FSS
    fun isProd() = miljø == Miljø.PROD_FSS
}
