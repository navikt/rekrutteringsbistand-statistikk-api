package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class LagreStatistikkTest {

    companion object {
        private val port = randomPort()
        private val database = TestDatabase()
        private val testRepository = TestRepository(database.dataSource)
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)
        private val basePath = basePath(port)

        init {
            start(database = database, port = port, mockOAuth2Server = mockOAuth2Server)
        }

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testRepository.slettAlleUtfall()
            testRepository.slettAlleStillinger()
        }
    }

    @Test
    fun `POST til kandidatutfall skal lagre til utfallstabellen`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfallMedUkjentHullICv)

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            setBody(kandidatutfallTilLagring)
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

    // Gitt POST med JSON-payload med ukjente felter
    // når mottar POST-request
    // så skal OpprettKandidatutfall-objektet bli lagret i db
    @Test
    fun `POST til kandidatutfall skal lagre til utfallstabellen også når JSON-payload har ukjente felter`() =
        runBlocking {
            val objectMapper = jacksonObjectMapper()
            val kandidatutfallTilLagring = etKandidatutfall
            val kandidatutfallJsonString: String = objectMapper.writeValueAsString(kandidatutfallTilLagring)
            val kandidatutfallJson: ObjectNode = objectMapper.readTree(kandidatutfallJsonString) as ObjectNode
            kandidatutfallJson.put("ukjentFelt", "anyString")

            val utvidetKandidatutfallJsonString = objectMapper.writeValueAsString(kandidatutfallJson)
            log.info("AAA " + utvidetKandidatutfallJsonString)
            val response: HttpResponse = client.post("$basePath/kandidatutfall") {
                setBody(listOf(utvidetKandidatutfallJsonString))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.Created)
            val lagredeUtfall = testRepository.hentUtfall()
            assertThat(lagredeUtfall).hasSize(1)
            val lagretUtfall = lagredeUtfall.first()
            assertThat(lagretUtfall.dbId).isNotNull()
            assertThat(lagretUtfall.aktorId).isEqualTo(kandidatutfallTilLagring.aktørId)
            assertThat(lagretUtfall.utfall.name).isEqualTo(kandidatutfallTilLagring.utfall.name)
            assertThat(lagretUtfall.navIdent).isEqualTo(kandidatutfallTilLagring.navIdent)
            assertThat(lagretUtfall.navKontor).isEqualTo(kandidatutfallTilLagring.navKontor)
            assertThat(lagretUtfall.kandidatlisteId.toString()).isEqualTo(kandidatutfallTilLagring.kandidatlisteId)
            assertThat(lagretUtfall.stillingsId.toString()).isEqualTo(kandidatutfallTilLagring.stillingsId)
            assertThat(lagretUtfall.synligKandidat).isEqualTo(kandidatutfallTilLagring.synligKandidat)
            assertThat(lagretUtfall.hullICv).isEqualTo(kandidatutfallTilLagring.harHullICv)
            assertThat(lagretUtfall.alder).isEqualTo(kandidatutfallTilLagring.alder)
            assertThat(lagretUtfall.tilretteleggingsbehov).isEqualTo(kandidatutfallTilLagring.tilretteleggingsbehov)
            assertThat(lagretUtfall.tidspunkt.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            )
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
