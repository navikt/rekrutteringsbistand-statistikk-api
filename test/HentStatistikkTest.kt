import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.statement.HttpResponse
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import db.TestDatabase
import db.TestRepository
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.rekrutteringsbistand.statistikk.StatistikkDto
import no.nav.rekrutteringsbistand.statistikk.db.Utfall
import org.junit.After
import org.junit.Test

@KtorExperimentalAPI
class HentStatistikkTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabase()
        private val repository = TestRepository(database.dataSource)
        private val port = randomPort()

        init {
            start(database, port)
        }
    }

    @Test
    fun `GET til statistikk skal returnere statistikk`() = runBlocking {
        val kandidatutfallTilLagring = listOf(
            etKandidatutfall.copy(utfall = Utfall.PRESENTERT.name),
            etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN.name)
        )
        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = kandidatutfallTilLagring
        }

        val response: StatistikkDto = client.get("$basePath/statistikk")
        // TODO: Finn ut av hvordan vi skal gjøre tellingen
        assertThat(response.antallPresentert).isEqualTo(1)
        assertThat(response.antallFåttJobben).isEqualTo(1)
    }

    @Test
    fun `GET til statistikk skal returnere unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache)
        val response: HttpResponse = uinnloggaClient.get("$basePath/statistikk")
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
    }
}
