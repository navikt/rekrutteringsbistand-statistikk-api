package statistikkapi

import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig

fun tokenSupportConfig(cluster: Cluster): TokenSupportConfig {
    val issuerConfigs = listOf(
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
            Cluster.LOKAL -> throw UnsupportedOperationException()
        },
        IssuerConfig(
            name = "azuread",
            discoveryUrl = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            acceptedAudience = listOf(System.getenv("AZURE_APP_CLIENT_ID")),
            cookieName = System.getenv("AZURE_OPENID_CONFIG_ISSUER")
        )
    )
    return TokenSupportConfig(*issuerConfigs.toTypedArray())
}


