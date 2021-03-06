package no.nav.rekrutteringsbistand.statistikk

import io.ktor.auth.Authentication
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerImpl
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaConfig
import no.nav.rekrutteringsbistand.statistikk.unleash.UnleashConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

@KtorExperimentalAPI
fun main() {
    val database = Database(Cluster.current)

    val tokenSupportConfig = tokenSupportConfig(Cluster.current)
    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaConfig.producerConfig())

    val applicationEngine = lagApplicationEngine(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        unleash = UnleashConfig.unleash
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
