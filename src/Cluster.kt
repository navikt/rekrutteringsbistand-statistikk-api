package no.nav.rekrutteringsbistand.statistikk

enum class Cluster {
    DEV_FSS, PROD_FSS, LOKAL;

    companion object {
        val current: Cluster by lazy {
            when (val c = System.getenv("NAIS_CLUSTER_NAME")) {
                "dev-fss" -> DEV_FSS
                "prod-fss" -> PROD_FSS
                null -> LOKAL
                else -> throw RuntimeException("Ukjent cluster: $c")
            }
        }
    }

    fun asString(): String = name.toLowerCase().replace("_", "-")
}
