package statistikkapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class AccessTokenProvider(private val config: Config, private val httpKlient: HttpClient = lagHttpKlient()) {
    private lateinit var bearerToken : BearerToken

    fun getBearerToken(): BearerToken {
        if (!this::bearerToken.isInitialized || bearerToken.erUtgått()) {
            bearerToken = nyttBearerToken()
        }
        return bearerToken
    }

    private fun nyttBearerToken() = runBlocking {
        val response: HttpResponse = httpKlient.submitForm(
            url = config.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_secret", config.azureClientSecret)
                append("client_id", config.azureClientId)
                append("scope", config.scope)
            }
        )
        val accessToken = jacksonObjectMapper().readValue(response.readText(), AccessToken::class.java)
        BearerToken(accessToken.access_token, LocalDateTime.now().plusSeconds(accessToken.expires_in.toLong()))
    }

    private data class AccessToken(
        val token_type: String,
        val expires_in: Int,
        val ext_expires_in: Int,
        val access_token: String
    )

    data class Config(
        val azureClientSecret: String,
        val azureClientId: String,
        val tokenEndpoint: String,
        val scope: String
    )

    companion object {
        private fun lagHttpKlient() = HttpClient() {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            engine {
                System.getenv("HTTP_PROXY")?.let {
                    log.info("Setter proxy")
                    this.proxy = ProxyBuilder.http(it)
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
