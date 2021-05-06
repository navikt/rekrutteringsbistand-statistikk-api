package statistikkapi

import io.ktor.auth.*
import io.ktor.util.*
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import statistikkapi.datakatalog.DatakatalogUrl
import statistikkapi.db.Database
import statistikkapi.kafka.DatavarehusKafkaProducerImpl
import statistikkapi.kafka.KafkaConfig
import statistikkapi.unleash.UnleashConfig

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

@KtorExperimentalAPI
fun main() {
    val database = Database(Cluster.current)

    val tokenSupportConfig = tokenSupportConfig(Cluster.current)
    val tokenValidationConfig: Authentication.Configuration.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaConfig.producerConfig())

    val datakatalogUrl = DatakatalogUrl(Cluster.current)

    val applicationEngine = lagApplicationEngine(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        unleash = UnleashConfig.unleash,
        url = datakatalogUrl
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
