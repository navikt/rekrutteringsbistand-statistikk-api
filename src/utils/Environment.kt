package no.nav.rekrutteringsbistand.statistikk.utils

private const val clusterEnvVar = "NAIS_CLUSTER_NAME"

enum class Cluster {
    LOKALT, DEV_FSS, PROD_FSS
}

data class Environment(
    val cluster: Cluster = when (System.getenv(clusterEnvVar)) {
        "dev-fss" -> Cluster.DEV_FSS
        "prod-fss" -> Cluster.PROD_FSS
        else -> Cluster.LOKALT
    }
) {
    fun isLokal() = cluster == Cluster.LOKALT
    fun isDev() = cluster == Cluster.DEV_FSS
    fun isProd() = cluster == Cluster.PROD_FSS
}


