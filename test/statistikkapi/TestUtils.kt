package statistikkapi

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.security.token.support.test.JwtTokenGenerator
import org.apache.http.HttpHeaders
import kotlin.random.Random

fun randomPort(): Int = Random.nextInt(1000, 9999)

fun innloggaHttpClient() = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.AUTHORIZATION, "Bearer ${JwtTokenGenerator.createSignedJWT(enNavIdent).serialize()}")
    }
}

fun basePath(port: Int) = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
