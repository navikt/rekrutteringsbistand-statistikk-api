package no.nav.rekrutteringsbistand.statistikk.auth

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.utils.Cluster
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig
import java.net.URL

@KtorExperimentalAPI
data class TokenValidationConfig(
    val config: ApplicationConfig,
    val resourceRetriever: ProxyAwareResourceRetriever
)

class TokenValidationUtil {
    @KtorExperimentalAPI
    companion object {
        fun tokenValidationConfig(
            issuerConfig: IssuerConfig,
            resourceRetriever: ProxyAwareResourceRetriever = ProxyAwareResourceRetriever(
                System.getenv("HTTP_PROXY")?.let { URL(it) }
            )
        ): TokenValidationConfig {
            return TokenValidationConfig(
                TokenSupportConfig(issuerConfig),
                resourceRetriever
            )
        }

        fun issuerConfig(cluster: Cluster): IssuerConfig =
            when (cluster) {
                Cluster.DEV_FSS -> IssuerConfig(
                    name = "isso",
                    discoveryUrl = "https://login.microsoftonline.com/NAVQ.onmicrosoft.com/.well-known/openid-configuration",
                    acceptedAudience = listOf("38e07d31-659d-4595-939a-f18dce3446c5"),
                    cookieName = "isso-idtoken"
                )
                Cluster.PROD_FSS -> IssuerConfig(
                    name = "isso",
                    discoveryUrl = "https://login.microsoftonline.com/navno.onmicrosoft.com/.well-known/openid-configuration",
                    acceptedAudience = listOf("9b4e07a3-4f4c-4bab-b866-87f62dff480d"),
                    cookieName = "isso-idtoken"
                )
        }
    }
}
