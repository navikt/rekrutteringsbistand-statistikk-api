package statistikkapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.apache.http.HttpHeaders
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.random.Random

fun hentToken(mockOAuth2Server: MockOAuth2Server, issuerId: String): String = mockOAuth2Server.issueToken(issuerId, "klient",
    DefaultOAuth2TokenCallback(
        issuerId = issuerId,
        claims = mapOf(
            Pair("NAVident", enNavIdent),
        ),
        audience = listOf("statistikk-api")
    )
).serialize()

fun randomPort(): Int = Random.nextInt(1000, 9999)

fun httpKlientMedBearerToken(mockOAuth2Server: MockOAuth2Server) = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.AUTHORIZATION, "Bearer ${hentToken(mockOAuth2Server, "azuread")}")
    }
}

fun basePath(port: Int) = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
