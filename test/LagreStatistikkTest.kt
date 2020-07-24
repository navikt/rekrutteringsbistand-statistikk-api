import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.rekrutteringsbistand.statistikk.log
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        database.hentUtfall().forEachIndexed { index, utfall ->
            assertThat(utfall.aktorId).isEqualTo(kandidatutfallTilLagring[index].aktørId)
            assertThat(utfall.utfall).isEqualTo(kandidatutfallTilLagring[index].utfall)
            assertThat(utfall.navIdent).isEqualTo(kandidatutfallTilLagring[index].navIdent)
            assertThat(utfall.navKontor).isEqualTo(kandidatutfallTilLagring[index].navKontor)
            assertThat(utfall.kandidatlisteId).isEqualTo(kandidatutfallTilLagring[index].kandidatlisteId)
            assertThat(utfall.stillingsId).isEqualTo(kandidatutfallTilLagring[index].stillingsId)
            assertThat(utfall.tidspunkt.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
        }
    }

    @Test
    fun `POST til kandidatutfall skal gi unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache)
        val response: HttpResponse = uinnloggaClient.post("$basePath/kandidatutfall")
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @After
    fun cleanUp() {
        database.slettAlleUtfall()
    }
}
