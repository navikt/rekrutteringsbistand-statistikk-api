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
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
        private val rapid = TestRapid()

        init {
            start(database = database, port = port, mockOAuth2Server = mockOAuth2Server)
        }

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testRepository.slettAlleUtfall()
            testRepository.slettAlleStillinger()
            rapid.reset()
        }
    }

    @Test
    fun `Når CV_DELT_VIA_REKRUTTERINGSBISTAND-hendelse leses skal noe skje med dette tullet`() {

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
    
    @Test
    fun `POST til kandidatutfall skal lagre til utfallstabellen også når JSON-payload har ukjente felter`() =
        runBlocking {
            val objectMapper = jacksonObjectMapper()
            val kandidatutfallTilLagring = etKandidatutfall
            val kandidatutfallJsonString: String = objectMapper.writeValueAsString(kandidatutfallTilLagring)
            val kandidatutfallJson: ObjectNode = objectMapper.readTree(kandidatutfallJsonString) as ObjectNode
            kandidatutfallJson.put("ukjentFelt", "anyString")
            val kandidatutfallJsonListe = listOf(kandidatutfallJson)

            val response: HttpResponse = client.post("$basePath/kandidatutfall") {
                setBody(kandidatutfallJsonListe)
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

    fun enMelding(aktørId: String = "dummyAktørId") = """
        {
          "@event_name": "kandidat.cv-delt-med-arbeidsgiver-via-rekrutteringsbistand",
          "kandidathendelse": {
            "type": "CV_DELT_VIA_REKRUTTERINGSBISTAND",
            "aktørId": "$aktørId",
            "organisasjonsnummer": "972112097",
            "kandidatlisteId": "34e81692-27ef-4fda-9b55-e17588f65061",
            "tidspunkt": "2022-08-15T12:10:43.698+02:00",
            "stillingsId": "a3c925af-ebf4-40d1-aeee-efc9259107a4",
            "utførtAvNavIdent": "Z994633",
            "utførtAvNavKontorKode": "0313",
            "synligKandidat": true
          }
        }
    """.trimIndent()
}
