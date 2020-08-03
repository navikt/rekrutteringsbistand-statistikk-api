package no.nav.rekrutteringsbistand.statistikk

import io.ktor.auth.Authentication
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseImpl
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerImpl
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerStub
import no.nav.rekrutteringsbistand.statistikk.kafka.KafkaConfig
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

@KtorExperimentalAPI
fun main() {
    val database = DatabaseImpl(Cluster.current)

    val tokenSupportConfig = tokenSupportConfig(Cluster.current)
    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val datavarehusKafkaProducer = when (Cluster.current) {
        Cluster.DEV_FSS -> DatavarehusKafkaProducerImpl(KafkaConfig.producerConfig())
        Cluster.PROD_FSS -> DatavarehusKafkaProducerStub()
    }

    val applicationEngine = lagApplicationEngine(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
