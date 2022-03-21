package statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.After
import org.junit.Test
import statistikkapi.db.TestDatabase
import statistikkapi.db.TestRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class LagreStatistikkTest {

    companion object {
        private val port = randomPort()
        private val database = TestDatabase()
        private val testRepository = TestRepository(database.dataSource)
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpclient(mockOAuth2Server)
        private val basePath = basePath(port)

        init {
            start(database = database, port = port, mockOAuth2Server = mockOAuth2Server)
        }
    }

    @Test
    fun `POST til kandidatutfall skal lagre til utfallstabellen`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfallMedUkjentHullICv)

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = kandidatutfallTilLagring
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        testRepository.hentUtfall().forEachIndexed { index, utfall ->
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
    fun `POST til kandidatutfall skal lagre til stillingstabellen`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfallMedUkjentHullICv)

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = kandidatutfallTilLagring
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(testRepository.hentAntallStillinger()).isEqualTo(1)
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
        testRepository.slettAlleUtfall()
        testRepository.slettAlleStillinger()
        mockOAuth2Server.shutdown()
    }
}
