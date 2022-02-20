package statistikkapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import kotlin.random.Random

private fun hentLoginServiceToken(mockOAuth2Server: MockOAuth2Server) = mockOAuth2Server.issueToken("isso-idtoken", "loginService",
    DefaultOAuth2TokenCallback(
        issuerId = "isso",
        claims = mapOf(
            Pair("NAVident", enNavIdent),
        ),
        audience = listOf("38e07d31-659d-4595-939a-f18dce3446c5")
    )
)

fun randomPort(): Int = Random.nextInt(1000, 9999)

fun httpClientMedLoginServiceToken(mockOAuth2Server: MockOAuth2Server) = HttpClient(Apache) {
    install(HttpCookies) {
        val token = hentLoginServiceToken(mockOAuth2Server)
        val cookie = Cookie("isso-idtoken", token.serialize())
        storage = ConstantCookiesStorage(cookie)
    }
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}

fun basePath(port: Int) = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
