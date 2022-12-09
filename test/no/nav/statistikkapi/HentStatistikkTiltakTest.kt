package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isZero
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall.*
import no.nav.statistikkapi.tiltak.TiltaksRepository
import org.junit.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class HentStatistikkTiltakTest {
    companion object {
        private val port = randomPort()
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)
        private val basePath = basePath(port)
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
        private val rapid = TestRapid()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid, port = port, mockOAuth2Server = mockOAuth2Server)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            mockOAuth2Server.shutdown()
        }
    }

    @Test
    fun `Gitt arbeidstrening-tiltak i basen så skal det telles`() {
        rapid.sendTestMessage(etArbeidstreningTiltak(aktørId1).tilRapidMelding())

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
        assertThat(actual.antallPresentert).isZero()
        assertThat(actual.antallFåttJobben).isZero()
    }

    @Test
    fun `Gitt to lønnstilskudd med ulik aktørid i basen så skal begge telles`() {
        rapid.sendTestMessage(etArbeidstreningTiltak(aktørId1).tilRapidMelding())

        rapid.sendTestMessage(etArbeidstreningTiltak(aktørId2).tilRapidMelding())

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(2)
        assertThat(actual.antallPresentert).isZero()
        assertThat(actual.antallFåttJobben).isZero()
    }

    @Test
    fun `Gitt ingen lønnstilskudd  så skal 0 returneres`() {

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isZero()
        assertThat(actual.antallPresentert).isZero()
        assertThat(actual.antallFåttJobben).isZero()
    }

    @Test
    fun `Gitt lønnstilskudd og formidling som allerede er registrert så skal kun tiltak telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = FATT_JOBBEN,
                aktørId = aktørId1,
                navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
            )
        )

        rapid.sendTestMessage(etArbeidstreningTiltak(aktørId1).tilRapidMelding())

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(1)
    }

    @Test
    fun `Gitt lønnstilskudd som lagres to ganger, så skal bare ett telles`() {
        val tiltak = etArbeidstreningTiltak(aktørId1)
        rapid.sendTestMessage(tiltak.tilRapidMelding())
        rapid.sendTestMessage(tiltak.tilRapidMelding())

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
    }

    private fun hentStatistikk(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        navKontor: String
    ): StatistikkOutboundDto = runBlocking {
        client.get("$basePath/statistikk") {
            leggTilQueryParametere(
                this,
                HentStatistikk(
                    fraOgMed = fraOgMed,
                    tilOgMed = tilOgMed,
                    navKontor = navKontor
                )
            )
        }.body()
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
        testRepository.slettAlleLønnstilskudd()
        rapid.reset()
    }

    private fun leggTilQueryParametere(httpRequestBuilder: HttpRequestBuilder, hentStatistikk: HentStatistikk) {
        httpRequestBuilder.url.parameters.apply {
            append(StatistikkParametere.fraOgMed, hentStatistikk.fraOgMed.toString())
            append(StatistikkParametere.tilOgMed, hentStatistikk.tilOgMed.toString())
            append(StatistikkParametere.navKontor, hentStatistikk.navKontor)
        }
    }

    fun etArbeidstreningTiltak(deltakerAktørId: String) = TiltaksRepository.OpprettTiltak(
        avtaleId = UUID.randomUUID(),
        deltakerAktørId = deltakerAktørId,
        deltakerFnr = "12121212121",
        enhetOppfolging = "NAV SKI",
        tiltakstype = "ARBEIDSTRENING",
        avtaleInngått = LocalDateTime.of(2022, 5, 3, 0, 0, 0).atZone(ZoneId.of("Europe/Oslo"))
    )

    private fun TiltaksRepository.OpprettTiltak.tilRapidMelding() = """
        {
          "tiltakstype":"$tiltakstype",
          "deltakerFnr": "$deltakerFnr",
          "aktørId": "$deltakerAktørId",
          "avtaleId":"$avtaleId",
          "enhetOppfolging":"$enhetOppfolging",
          "avtaleInngått": "${LocalDateTime.now()}"
        }
        """.trimIndent()
}
