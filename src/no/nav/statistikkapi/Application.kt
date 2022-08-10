package no.nav.statistikkapi

import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.Database
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerImpl
import no.nav.statistikkapi.kafka.KafkaConfig
import no.nav.statistikkapi.stillinger.ElasticSearchKlientImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.rekrutteringsbistand.statistikk")

fun main() {
    val database = Database(Cluster.current)

    val tokenSupportConfig = TokenSupportConfig(
        IssuerConfig(
            name = "azuread",
            discoveryUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            acceptedAudience = listOf(System.getenv("AZURE_APP_CLIENT_ID")),
            cookieName = System.getenv("AZURE_OPENID_CONFIG_ISSUER")
        )
    )
    val tokenValidationConfig: AuthenticationConfig.() -> Unit = {
        tokenValidationSupport(config = tokenSupportConfig)
    }

    val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(KafkaConfig.producerConfig())

    val stillingssokProxyAccessTokenClient = AccessTokenProvider(
        config = AccessTokenProvider.Config(
            azureClientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
            azureClientId = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            scope = ElasticSearchKlientImpl.stillingssokProxyScope
        )
    )
    val elasticSearchKlient =
        ElasticSearchKlientImpl(tokenProvider = stillingssokProxyAccessTokenClient::getBearerToken)

    var ktor: Application? = null

    val rapid = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(System.getenv())
    ).withKtorModule {
        ktor = this
    }.build()
    rapid.start()

    startApp(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        elasticSearchKlient = elasticSearchKlient,
        ktor = ktor!!,
        rapidsConnection = rapid
    )
}
