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
import no.nav.statistikkapi.tiltak.Tiltakstype
import org.junit.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1))

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
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1))

        rapid.sendTestMessage(tiltakRapidMelding(aktørId2))

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

        rapid.sendTestMessage(tiltakRapidMelding(aktørId1))

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
    fun `Gitt tiltak som lagres to ganger, nyeste sendes sist, så skal bare nyeste telles`() {
        val tid1 = LocalDate.of(2022, 1, 1).atStartOfDay()
        val tid2 =  LocalDate.of(2022, 2, 1).atStartOfDay()
        val avtaleId = UUID.randomUUID()

        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleId = avtaleId, tiltakstype = "MIDLERTIDIG_LONNSTILSKUDD", sistEndret = tid1))
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleId = avtaleId, tiltakstype = "ARBEIDSTRENING", sistEndret = tid2))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
        assertThat(actual.tiltakstatistikk.antallFåttJobbenArbeidstrening).isEqualTo(1)
        assertThat(actual.tiltakstatistikk.antallFåttJobbenLønnstilskudd).isEqualTo(0)
    }

    @Test
    fun `Gitt tiltak som lagres to ganger, nyeste sendes først,  så skal bare nyeste telles`() {
        val tid1 =  LocalDate.of(2022, 1, 1).atStartOfDay()
        val tid2 =  LocalDate.of(2022, 2, 1).atStartOfDay()
        val avtaleId = UUID.randomUUID()

        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleId = avtaleId, tiltakstype = "MIDLERTIDIG_LONNSTILSKUDD", sistEndret = tid2))
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleId = avtaleId, tiltakstype = "ARBEIDSTRENING", sistEndret = tid1))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
        assertThat(actual.tiltakstatistikk.antallFåttJobbenArbeidstrening).isEqualTo(0)
        assertThat(actual.tiltakstatistikk.antallFåttJobbenLønnstilskudd).isEqualTo(1)
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
        avtaleInngått = LocalDateTime.of(2022, 5, 3, 0, 0, 0).atZone(ZoneId.of("Europe/Oslo")),
        sistEndret = LocalDateTime.of(2022, 5, 2, 0, 0, 0).atZone(ZoneId.of("Europe/Oslo")),
    )

    private fun tiltakRapidMelding(
        deltakerAktørId: String,
        avtaleId: UUID = UUID.randomUUID(),
        deltakerFnr: String = "12121212121",
        enhetOppfolging: String = "NAV SKI",
        tiltakstype: String = "ARBEIDSTRENING",
        avtaleInngått: LocalDateTime = LocalDateTime.of(2022, 5, 3, 0, 0, 0),
        sistEndret: LocalDateTime = LocalDateTime.of(2022, 5, 2, 0, 0, 0),
    ) = """
        {
          "tiltakstype":"$tiltakstype",
          "deltakerFnr": "$deltakerFnr",
          "aktørId": "$deltakerAktørId",
          "avtaleId":"$avtaleId",
          "enhetOppfolging":"$enhetOppfolging",
          "avtaleInngått": "$avtaleInngått",
          "sistEndret": "$sistEndret"
        }
    """.trimIndent()
}
