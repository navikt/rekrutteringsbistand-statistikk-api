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
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
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
        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = TextContent(jacksonObjectMapper().writeValueAsString(listOf(etKandidatutfall)), ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        database.hentUtfall().forEach {
            assertEquals(etKandidatutfall, it)
        }
    }
}
