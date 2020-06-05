import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import db.TestDatabase
import org.junit.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class LagreStatistikkTest {

    private val basePath = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache) {
        install(HttpCookies) {
            storage = ConstantCookiesStorage(lagCookie())
        }
    }

    companion object {
        private val database = TestDatabase()
        private val port = randomPort()

        init {
            start(database, port)
        }
    }

    @Test
    fun `POST til kandidatutfall skal lagre til databasen`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(kandidatutfallTilLagring, database.hentUtfall())
    }

    @Test
    fun `POST til kandidatutfall skal gi unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache)
        val response: HttpResponse = uinnloggaClient.post("$basePath/kandidatutfall")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun tilJson(objekt: Any): String =
        jacksonObjectMapper().writeValueAsString(objekt)
}
