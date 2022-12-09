package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isZero
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall.*
import no.nav.statistikkapi.tiltak.TiltaksRepository
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HentStatistikkTiltakTest {


    companion object {
        private val port = randomPort()
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)
        private val basePath = basePath(port)
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val tiltaksRepository = TiltaksRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)

        init {
            start(
                database = database,
                port = port,
                mockOAuth2Server = mockOAuth2Server
            )
        }
    }

    @Test
    fun `Gitt arbeidstrening-tiltak i basen så skal det telles`() {
        tiltaksRepository.lagreTiltak(
            etArbeidstreningTiltak(aktørId1)
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).navkontor
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(1)
        assertThat(actual.antallPresentert).isZero()
        assertThat(actual.antallFåttJobben).isZero()
    }

    @Test
    fun `Gitt to lønnstilskudd med ulik aktørid i basen så skal begge telles`() {
        tiltaksRepository.lagreTiltak(
            etArbeidstreningTiltak(aktørId1)
        )

        tiltaksRepository.lagreTiltak(
            etArbeidstreningTiltak(aktørId2)
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).navkontor
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
            navKontor = etArbeidstreningTiltak(aktørId1).navkontor
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isZero()
        assertThat(actual.antallPresentert).isZero()
        assertThat(actual.antallFåttJobben).isZero()
    }

    @Test
    fun `Gitt lønnstilskudd som allerede er registrert så skal det ikke telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = FATT_JOBBEN,
                aktørId = aktørId1,
                navKontor = etArbeidstreningTiltak(aktørId1).navkontor
            )
        )

        tiltaksRepository.lagreTiltak(
            etArbeidstreningTiltak(aktørId1)
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2022, 1, 1),
            tilOgMed = LocalDate.of(2022, 12, 31),
            navKontor = etArbeidstreningTiltak(aktørId1).navkontor
        )

        assertThat(actual.tiltakstatistikk.antallFåttJobben).isEqualTo(0)
        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(1)
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
        mockOAuth2Server.shutdown()
    }

    private fun leggTilQueryParametere(httpRequestBuilder: HttpRequestBuilder, hentStatistikk: HentStatistikk) {
        httpRequestBuilder.url.parameters.apply {
            append(StatistikkParametere.fraOgMed, hentStatistikk.fraOgMed.toString())
            append(StatistikkParametere.tilOgMed, hentStatistikk.tilOgMed.toString())
            append(StatistikkParametere.navKontor, hentStatistikk.navKontor)
        }
    }
}
