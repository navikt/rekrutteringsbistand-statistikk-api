package statistikkapi
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import statistikkapi.db.TestDatabase
import statistikkapi.db.TestRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@KtorExperimentalAPI
class LagreStatistikkTest {

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
    fun `POST til kandidatutfall skal lagre til databasen`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfallMedUkjentHullICv)

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = kandidatutfallTilLagring
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        repository.hentUtfall().forEachIndexed { index, utfall ->
            assertThat(utfall.dbId).isNotNull()
            assertThat(utfall.aktorId).isEqualTo(kandidatutfallTilLagring[index].aktørId)
            assertThat(utfall.utfall.name).isEqualTo(kandidatutfallTilLagring[index].utfall.name)
            assertThat(utfall.navIdent).isEqualTo(kandidatutfallTilLagring[index].navIdent)
            assertThat(utfall.navKontor).isEqualTo(kandidatutfallTilLagring[index].navKontor)
            assertThat(utfall.kandidatlisteId.toString()).isEqualTo(kandidatutfallTilLagring[index].kandidatlisteId)
            assertThat(utfall.stillingsId.toString()).isEqualTo(kandidatutfallTilLagring[index].stillingsId)
            assertThat(utfall.synligKandidat).isEqualTo(kandidatutfallTilLagring[index].synligKandidat)
            assertThat(utfall.hullICv).isEqualTo(kandidatutfallTilLagring[index].harHullICv)
            assertThat(utfall.alder).isEqualTo(kandidatutfallTilLagring[index].alder)
            assertThat(utfall.tilretteleggingsbehov).isEqualTo(kandidatutfallTilLagring[index].tilretteleggingsbehov)
            assertThat(utfall.tidspunkt.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            )
        }
    }

    @Test
    fun `POST til kandidatutfall skal gi unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache) {
            expectSuccess = false
        }
        val response: HttpResponse = uinnloggaClient.post("$basePath/kandidatutfall")
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
    }
}
