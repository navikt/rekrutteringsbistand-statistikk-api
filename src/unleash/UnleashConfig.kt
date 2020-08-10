package no.nav.rekrutteringsbistand.statistikk.unleash

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import no.nav.rekrutteringsbistand.statistikk.Cluster

const val SEND_KANDIDATUTFALL_PÅ_KAFKA = "rekrutteringsbistand-statistikk-api.send-kandidatutfall-paa-kafka-til-dvh"

class UnleashConfig {

    companion object {
        private val config: UnleashConfig = UnleashConfig.builder()
            .appName("rekrutteringsbistand-statistikk-api")
            .instanceId("rekrutteringsbistand-statistikk-api-${Cluster.current}")
            .unleashAPI("https://unleash.nais.adeo.no/api/")
            .build()

        val unleash: Unleash = DefaultUnleash(config, ByClusterStrategy(Cluster.current))

        class ByClusterStrategy(private val currentCluster: Cluster) : Strategy {
            override fun getName(): String = "byCluster"

            override fun isEnabled(parameters: Map<String, String>?): Boolean {
                val clustersParameter = parameters?.get("cluster") ?: return false
                val alleClustere = clustersParameter.split(",").map { it.trim() }.map { it.toLowerCase() }.toList()
                return alleClustere.contains(currentCluster.asString())
            }
        }
    }
}
