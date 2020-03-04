package no.nav.rekrutteringsbistand.statistikk.utils

import java.lang.RuntimeException

val clusterEnvVar: String? = System.getenv("NAIS_CLUSTER_NAME")

enum class Cluster {
    DEV_FSS, PROD_FSS
}

data class Environment(
    val cluster: Cluster = when (clusterEnvVar) {
        "dev-fss" -> Cluster.DEV_FSS
        "prod-fss" -> Cluster.PROD_FSS
        else -> throw RuntimeException("Ukjent cluster: $clusterEnvVar")
    }
) {
    fun isDev() = cluster == Cluster.DEV_FSS
    fun isProd() = cluster == Cluster.PROD_FSS
}
