import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.*
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.*
import io.ktor.http.*
import no.nav.security.token.support.test.JwtTokenGenerator
import kotlin.random.Random

fun lagCookie(): Cookie {
    val token: SignedJWT = JwtTokenGenerator.createSignedJWT(enNavIdent)
    return Cookie("isso-idtoken", token.serialize())
}

fun randomPort(): Int = Random.nextInt(1000, 9999)

fun innloggaHttpClient() = HttpClient(Apache) {
    install(HttpCookies) {
        storage = ConstantCookiesStorage(lagCookie())
    }
    install(JsonFeature) {
        serializer = JacksonSerializer()
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}

fun basePath(port: Int) = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
