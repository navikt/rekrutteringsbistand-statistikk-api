package no.nav.rekrutteringsbistand.statistikk

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.After
import org.junit.Test
import kotlin.test.assertNotEquals

class KandidatutfallTest {

    private val basePath = "http://localhost:8080/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache) {
        install(HttpCookies) {
            storage = ConstantCookiesStorage(lagCookie())
        }
    }

    @KtorExperimentalAPI
    companion object {
        init {
            main()
        }
    }

    @Test
    fun `POST til kandidatutfall skal lagre i database`() = runBlocking {
        val response: HttpResponse = client.post {
            url("$basePath/kandidatutfall")
        }
        assertNotEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @After
    fun tearDown() {
        client.close()
    }
}

fun lagCookie(): Cookie {
    val subject = "X123456"
    val token: SignedJWT = JwtTokenGenerator.createSignedJWT(subject)
    return Cookie("isso-idtoken", token.serialize())
}

