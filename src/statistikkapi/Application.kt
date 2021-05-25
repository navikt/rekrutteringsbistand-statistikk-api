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

    val stillingssokProxyAccessTokenClient = AccessTokenProvider(config = AccessTokenProvider.Config(
        azureClientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
        azureClientId = System.getenv("AZURE_APP_CLIENT_ID"),
        tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
    ))
    val elasticSearchKlient = ElasticSearchKlientImpl(tokenProvider = stillingssokProxyAccessTokenClient::getBearerToken)

    val applicationEngine = lagApplicationEngine(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        url = datakatalogUrl,
        elasticSearchKlient = elasticSearchKlient
    )
    applicationEngine.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
