package no.nav.statistikkapi

import io.ktor.server.auth.*
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.statistikkapi.db.Database
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducerImpl
import no.nav.statistikkapi.kafka.KafkaConfig
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.ElasticSearchKlientImpl
import no.nav.statistikkapi.stillinger.StillingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

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

    val stillingRepository = StillingRepository(database.dataSource)
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

    val rapidsConnection = lagRapidsApplication(
        dataSource = database.dataSource,
        tokenValidationConfig = tokenValidationConfig,
        datavarehusKafkaProducer = datavarehusKafkaProducer,
        elasticSearchKlient = elasticSearchKlient,
        stillingRepository = stillingRepository
    )
    rapidsConnection.start()
    log.info("Applikasjon startet i miljø: ${Cluster.current}")
}
