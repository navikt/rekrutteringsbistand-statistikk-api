package no.nav.rekrutteringsbistand.statistikk.kafka

import io.micrometer.core.instrument.Metrics
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log

fun hentUsendteUtfallOgSendPåKafka(repository: Repository, kafkaProducer: DatavarehusKafkaProducer) = Runnable {
// TODO
//    if (unleash.isEnabled("rekrutteringsbistand-statistikk-api.send-kandidatutfall-paa-kafka-til-dvh")) { ...

    val skalSendes = repository.hentUsendteUtfall()

    skalSendes.forEach {
        repository.registrerSendtForsøk(it)
        try {
            kafkaProducer.send(it)
            repository.registrerSomSendt(it)
        } catch (e: Exception) {
            log.error("Prøvde å sende melding på Kafka til Datavarehus om et kandidatutfall", e)
            Metrics.counter(
                "rekrutteringsbistand.statistikk.kafka.feilet",
                "antallSendtForsøk", it.antallSendtForsøk.toString()
            ).increment()
            return@Runnable
        }
    }
}

//////////////////////////////////////////////////////
// TODO Førsteutkast, rydd
val config: UnleashConfig = UnleashConfig.builder()
    .appName("rekrutteringsbistand-statistikk-api")
    .instanceId("TODO Hva er dette?") // TODO
    .unleashAPI("https://unleash.nais.adeo.no/api/")
    .build()

object ByClusterStrategy : Strategy {
    override fun getName(): String {
        return "byCluster"
    }

    override fun isEnabled(parameters: Map<String, String>?): Boolean {
        val clustersParameter = parameters?.get("cluster") ?: return false
        val alleClustere = clustersParameter.split(",").map { it.trim() }.map { it.toLowerCase() }.toList()
        return alleClustere.contains(Cluster.current.name.toLowerCase())
    }

}

val unleash: Unleash = DefaultUnleash(config, ByClusterStrategy)
///////////////////////////////////
