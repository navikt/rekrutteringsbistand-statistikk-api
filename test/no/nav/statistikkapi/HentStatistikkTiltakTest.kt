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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.*
import java.util.*

@RunWith(value = Parameterized::class)
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
        private lateinit var defaultTimeZone: TimeZone

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            defaultTimeZone = TimeZone.getDefault()
            start(database = database, rapid = rapid, port = port, mockOAuth2Server = mockOAuth2Server)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            mockOAuth2Server.shutdown()
        }

        @Parameterized.Parameters(name = "{index}: timezone{0} - {2}")
        @JvmStatic
        fun data(): Collection<TimeZone> {
            return listOf(
                    TimeZone.getDefault(),
                    TimeZone.getTimeZone(ZoneId.of("Europe/Oslo")),
                    TimeZone.getTimeZone(ZoneId.of("UTC"))
            )
        }
    }

    @Parameterized.Parameter
    lateinit var timezone: TimeZone

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
    fun `Gitt tiltak med slutt_av_hendelseskjede satt til true skal det ikke lagres i database`() {
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, sluttAvHendelseskjede = true))

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
                navKontor = etArbeidstreningTiltak(aktørId1).enhetOppfolging,
                tidspunktForHendelsen = LocalDate.of(2022, 12, 19).atStartOfDay().atZone(timezone.toZoneId())
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
        val tid1 =  ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val tid2 =  ZonedDateTime.of(2022, 2, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val avtaleId = UUID.randomUUID()
        val avtaleInngått = LocalDateTime.of(2022, 6, 3, 0, 0, 0)

        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleInngått = avtaleInngått, avtaleId = avtaleId, tiltakstype = "MIDLERTIDIG_LONNSTILSKUDD", sistEndret = tid1))
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleInngått = avtaleInngått, avtaleId = avtaleId, tiltakstype = "ARBEIDSTRENING", sistEndret = tid2))

        val tiltaksrad = testRepository.hentTiltak()
        assertThat(tiltaksrad.tiltakstype).isEqualTo("ARBEIDSTRENING")
        assertThat(tiltaksrad.avtaleInngått).isEqualTo(avtaleInngått.atOslo())
        assertThat(tiltaksrad.sistEndret).isEqualTo(tid2.atOslo())

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
        val tid1 =  ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val tid2 =  ZonedDateTime.of(2022, 2, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val avtaleId = UUID.randomUUID()
        val avtaleInngått = LocalDateTime.of(2022, 6, 3, 0, 0, 0)

        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleInngått = avtaleInngått, avtaleId = avtaleId, tiltakstype = "MIDLERTIDIG_LONNSTILSKUDD", sistEndret = tid2))
        rapid.sendTestMessage(tiltakRapidMelding(aktørId1, avtaleInngått = avtaleInngått, avtaleId = avtaleId, tiltakstype = "ARBEIDSTRENING", sistEndret = tid1))

        val tiltaksrad = testRepository.hentTiltak()
        assertThat(tiltaksrad.tiltakstype).isEqualTo("MIDLERTIDIG_LONNSTILSKUDD")
        assertThat(tiltaksrad.avtaleInngått).isEqualTo(avtaleInngått.atOslo())
        assertThat(tiltaksrad.sistEndret).isEqualTo(tid2.atOslo())

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

    @Before
    fun setUp() {
        TimeZone.setDefault(timezone)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
        testRepository.slettAlleLønnstilskudd()
        rapid.reset()
        TimeZone.setDefault(defaultTimeZone)
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
        avtaleInngått = LocalDateTime.of(2022, 5, 3, 0, 0, 0).atOslo(),
        sistEndret = LocalDateTime.of(2022, 5, 2, 0, 0, 0).atOslo(),
    )

    private fun tiltakRapidMelding(
        deltakerAktørId: String,
        avtaleId: UUID = UUID.randomUUID(),
        deltakerFnr: String = "12121212121",
        enhetOppfolging: String = "NAV SKI",
        tiltakstype: String = "ARBEIDSTRENING",
        avtaleInngått: LocalDateTime = LocalDateTime.of(2022, 5, 3, 0, 0, 0),
        sistEndret: Instant = ZonedDateTime.of(2022, 5, 2, 0, 0, 0,0,ZoneId.of("UTC")).toInstant(),
        sluttAvHendelseskjede: Boolean? = null
    ) = """
        {
          "tiltakstype":"$tiltakstype",
          "deltakerFnr": "$deltakerFnr",
          "aktørId": "$deltakerAktørId",
          "avtaleId":"$avtaleId",
          "enhetOppfolging":"$enhetOppfolging",
          "avtaleInngått": "$avtaleInngått",
          "sistEndret": "$sistEndret"
          ${if(sluttAvHendelseskjede == null) "" else """, "@slutt_av_hendelseskjede": $sluttAvHendelseskjede """}
        }
    """.trimIndent()
}
