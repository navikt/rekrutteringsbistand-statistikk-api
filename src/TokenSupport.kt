package no.nav.rekrutteringsbistand.statistikk

import io.ktor.util.KtorExperimentalAPI
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig


@KtorExperimentalAPI
fun tokenSupportConfig(cluster: Cluster): TokenSupportConfig {
    val issuerConfig = when (cluster) {
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
    return TokenSupportConfig(issuerConfig)
}


