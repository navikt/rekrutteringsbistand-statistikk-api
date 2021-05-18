package statistikkapi.stillinger.autentisering

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import statistikkapi.Cluster
import statistikkapi.log
import java.time.LocalDateTime

class StillingssokProxyAccessTokenKlient(private val config: AuthenticationConfig,
                                         private val httpKlient: HttpClient = lagHttpKlient()) {
    private var bearerToken = nyttBearerToken()

    fun getBearerToken(): BearerToken {
        if (bearerToken.erUtgått()) {
            bearerToken = nyttBearerToken()
        }
        return bearerToken
    }

    private fun nyttBearerToken() = runBlocking {
        val stillingsSokProxyCluster = if (Cluster.current == Cluster.PROD_FSS) "prod-gcp" else "dev-gcp"

        val accessToken = httpKlient.submitForm<AccessToken>(
            url = "https://login.microsoftonline.com/${config.azureTenantId}/oauth2/v2.0/token",
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_secret", config.azureClientSecret)
                append("client_id", config.azureClientId)
                append("scope", "api://${stillingsSokProxyCluster}.arbeidsgiver.rekrutteringsbistand-stillingssok-proxy/.default")
            }
        )
        log.info("Har hentet access token for stillingssok-proxy")
        BearerToken(accessToken.access_token, LocalDateTime.now().plusSeconds(accessToken.expires_in.toLong()))
    }

    data class AuthenticationConfig(
        val azureClientSecret: String,
        val azureClientId: String,
        val azureTenantId: String,
    )

    private data class AccessToken(
        val token_type: String,
        val expires_in: Int,
        val ext_expires_in: Int,
        val access_token: String
    )

    companion object {
        private fun lagHttpKlient() = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(JavaTimeModule())
                }
            }
        }
    }
}

class BearerToken(
    private val accessToken: String,
    private val utgår: LocalDateTime,
) {
    private val utløpsmarginSekunder = 30L
    fun erUtgått() = utgår.minusSeconds(utløpsmarginSekunder).isBefore(LocalDateTime.now())
    fun leggTilBearerToken(): HeadersBuilder.() -> Unit = {
        this.apply { append(HttpHeaders.Authorization, "Bearer $accessToken") }
    }
}
