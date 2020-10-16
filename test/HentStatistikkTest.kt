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
import no.nav.rekrutteringsbistand.statistikk.StatistikkInboundDto
import no.nav.rekrutteringsbistand.statistikk.StatistikkOutboundDto
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Utfall
import org.junit.After
import org.junit.Test
import java.time.LocalDate

@KtorExperimentalAPI
class HentStatistikkTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabase()
        private val repository = Repository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
        private val port = randomPort()

        init {
            start(database, port)
        }
    }

    @Test
    fun `GET til statistikk skal returnere telling for siste registrerte formidling pr kandidat i gitt tidperiode`() = runBlocking {
        listOf(
            Pair(etKandidatutfall.copy(utfall = Utfall.PRESENTERT.name, aktørId = "1"), LocalDate.of(2020, 10, 1).atStartOfDay()),
            Pair(etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN.name, aktørId = "1"), LocalDate.of(2020, 10, 31).atStartOfDay()),
            Pair(etKandidatutfall.copy(utfall = Utfall.PRESENTERT.name, aktørId = "2"), LocalDate.of(2020, 10, 1).atStartOfDay()),
            Pair(etKandidatutfall.copy(utfall = Utfall.FATT_JOBBEN.name, aktørId = "2"), LocalDate.of(2020, 11, 1).atStartOfDay())
        ).forEach {
            repository.lagreUtfall(it.first, it.second)
        }

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31)
            )
        }
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
        testRepository.slettAlleUtfall()
    }
}
