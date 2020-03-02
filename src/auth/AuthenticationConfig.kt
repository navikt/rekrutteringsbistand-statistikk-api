package no.nav.rekrutteringsbistand.statistikk.auth

import io.ktor.util.KtorExperimentalAPI
import no.nav.rekrutteringsbistand.statistikk.utils.Cluster
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.security.token.support.ktor.IssuerConfig
import no.nav.security.token.support.ktor.TokenSupportConfig

@KtorExperimentalAPI
class AuthenticationConfig {

    companion object {
        fun tokenSupportConfig(env: Environment): TokenSupportConfig {
            val issuerConfig = when (env.cluster) {
                Cluster.LOKALT -> IssuerConfig(
                    name = "isso",
                    discoveryUrl = "http://metadata",
                    acceptedAudience = listOf("aud-localhost", "aud-isso"),
                    cookieName = "isso-idtoken"
                )
                Cluster.DEV_FSS -> IssuerConfig(
                    name = "isso",
                    discoveryUrl = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0/.well-known/openid-configuration",
                    acceptedAudience = listOf("966ac572-f5b7-4bbe-aa88-c76419c0f851"),
                    cookieName = "isso-idtoken"
                )
                Cluster.PROD_FSS -> IssuerConfig(
                    name = "isso",
                    discoveryUrl = "https://login.microsoftonline.com/62366534-1ec3-4962-8869-9b5535279d0b/v2.0/.well-known/openid-configuration",
                    acceptedAudience = listOf("62366534-1ec3-4962-8869-9b5535279d0b"),
                    cookieName = "isso-idtoken"
                )
            }
            return TokenSupportConfig(issuerConfig)
        }
    }
}
