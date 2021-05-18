package statistikkapi.stillinger.autentisering

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class StillingssokProxyAccessTokenClient(private val config: AuthenticationConfig,
                                         private val httpKlient: HttpClient = HttpClient(Apache)
) {
    private var bearerToken: BearerToken

    init {
        bearerToken = getNewBearerToken()
    }

    fun getBearerToken(scope: String): BearerToken {
        if (bearerToken.expired()) {
            bearerToken = getNewBearerToken()
        }
        return bearerToken
    }

    private fun getNewBearerToken() = runBlocking {
        val formData = listOf(
                "grant_type" to "client_credentials",
                "client_secret" to config.azureClientSecret,
                "client_id" to config.azureClientId,
                "scope" to config.stillingssokProxyClientId
        )

        val accessToken = httpKlient.post<AccessToken>("https://login.microsoftonline.com/${config.azureTenantId}/oauth2/v2.0/token") {
            body = formData
        }

        BearerToken(accessToken.access_token, LocalDateTime.now().plusSeconds(accessToken.expires_in.toLong()))
    }

    data class AuthenticationConfig(
        val azureClientSecret: String,
        val azureClientId: String,
        val azureTenantId: String,
        val stillingssokProxyClientId: String
    )

    private data class AccessToken(
        val token_type: String,
        val expires_in: Int,
        val ext_expires_in: Int,
        val access_token: String
    )
}

class BearerToken(
    private val accessToken: String,
    private val expires: LocalDateTime,
) {
    private val utløpsmarginSekunder = 30L
    fun expired() = expires.minusSeconds(utløpsmarginSekunder).isBefore(LocalDateTime.now())
    fun appendBearerToken(): HeadersBuilder.() -> Unit = {
        this.apply { append(HttpHeaders.Authorization, "Bearer $accessToken") }
    }
}
