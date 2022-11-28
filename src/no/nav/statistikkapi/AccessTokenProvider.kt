package no.nav.statistikkapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class AccessTokenProvider(private val config: Config, private val httpKlient: HttpClient = lagHttpKlient()) {
    private lateinit var bearerToken: BearerToken

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
                // Note that the StringValuesBuilder class that exposes the append function is incorrectly marked with
                // the InternalAPI annotation. This issue will be fixed in v2.0.0. As a workaround, you can add the
                // @OptIn(InternalAPI::class) annotation to explicitly opt-in to use this API.
                //  https://ktor.io/docs/request.html#headers
                append("grant_type", "client_credentials")
                append("client_secret", config.azureClientSecret)
                append("client_id", config.azureClientId)
                append("scope", config.scope)
            }
        )
        val accessToken = jacksonObjectMapper().readValue(response.bodyAsText(), AccessToken::class.java)
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
        private fun lagHttpKlient() = HttpClient {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            engine {
                System.getenv("HTTP_PROXY")?.let {
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
        this.apply {
            // Note that the StringValuesBuilder class that exposes the append function is incorrectly marked with
            // the InternalAPI annotation. This issue will be fixed in v2.0.0. As a workaround, you can add the
            // @OptIn(InternalAPI::class) annotation to explicitly opt-in to use this API.
            //  https://ktor.io/docs/request.html#headers
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
}
