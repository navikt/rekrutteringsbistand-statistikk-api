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
import statistikkapi.stillinger.ElasticSearchKlientImpl
import statistikkapi.stillinger.autentisering.StillingssokProxyAccessTokenKlient
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

    val stillingssokProxyAccessTokenClient = StillingssokProxyAccessTokenKlient(
        config = StillingssokProxyAccessTokenKlient.AuthenticationConfig(
            azureClientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
            azureClientId = System.getenv("AZURE_APP_CLIENT_ID"),
            azureTenantId = System.getenv("AZURE_APP_TENANT_ID")
        ))
    val elasticSearchKlient = ElasticSearchKlientImpl { stillingssokProxyAccessTokenClient.getBearerToken() }

    val applicationEngine = lagApplicationEngine(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        unleash = UnleashConfig.unleash,
        url = datakatalogUrl,
        elasticSearchKlient = elasticSearchKlient
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
