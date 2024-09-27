package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isZero
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Innsatsgruppe.BFORM
import no.nav.statistikkapi.kandidatutfall.Innsatsgruppe.Companion.erIkkeStandardinnsats
import no.nav.statistikkapi.kandidatutfall.Innsatsgruppe.IKVAL
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall.*
import org.apache.http.HttpHeaders
import org.junit.After
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.ZonedDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HentStatistikkTest {

    companion object {
        private val port = randomPort()
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)

        private val basePath = basePath(port)
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)

        init {
            start(
                database = database,
                port = port,
                mockOAuth2Server = mockOAuth2Server
            )
        }

        private fun assertIsZero(ant: AntallDto) {
            assertThat(ant.totalt).isZero()
            assertThat(ant.innsatsgruppeIkkeStandard).isZero()
            assertThat(ant.under30år).isZero()
        }

        private fun tidspunkt(år: Int, måned: Int, dag: Int): ZonedDateTime =
            LocalDate.of(år, måned, dag).atStartOfDay().atOslo()

    }


    @Test
    fun `Siste registrerte presentering på en kandidat og kandidatliste skal telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = PRESENTERT, tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        val actual: StatistikkOutboundDto = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }


    @Test
    fun `Siste registrerte fått jobben på en kandidat og kandidatliste skal telles som kun fått jobben`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(
                aktørId = "1234",
                utfall = FATT_JOBBEN,
                tidspunktForHendelsen = tidspunkt(2020, 10, 15)
            )
        )


        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antFåttJobben.totalt).isEqualTo(2)
        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
    }


    @Test
    fun `Ikke presentert skal ikke telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = IKKE_PRESENTERT, tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(0)
    }

    @Test
    fun `Registrert formidling innen tidsperiode skal telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }

    @Test
    fun `Registrert formidling før eller etter gitt tidsperiode skal ikke telles`() {
        repository.lagreUtfall(etKandidatutfall.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1)))
        repository.lagreUtfall(etKandidatutfall.copy(tidspunktForHendelsen = tidspunkt(2020, 5, 1)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 2, 1),
            tilOgMed = LocalDate.of(2020, 4, 1),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(0)
    }

    @Test
    fun `Registrert utfall på samme kandidat på to kandidatlister skal gi to tellinger`() {
        val kandidatutfall1 =
            etKandidatutfall.copy(kandidatlisteId = "1", tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        val kandidatutfall2 =
            kandidatutfall1.copy(kandidatlisteId = "2", tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(kandidatutfall1)
        repository.lagreUtfall(kandidatutfall2)

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(2)
    }

    @Test
    fun `Registrerte utfall på to kandidater på en kandidatliste skal gi to tellinger`() {
        val kandidatutfall1 = etKandidatutfall.copy(aktørId = "1", tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        val kandidatutfall2 = kandidatutfall1.copy(aktørId = "2", tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)

        repository.lagreUtfall(kandidatutfall1)
        repository.lagreUtfall(kandidatutfall2)

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(2)
    }

    @Test
    fun `Presentert og fått jobben på samme kandidat, samme kandidatliste og samme tidspunkt skal telles som presentert og fått jobben`() {
        val kandidatutfallPresentert = etKandidatutfall.copy(utfall = PRESENTERT)
            .copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        val kandidatutfallFåttJobben = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        assertThat(kandidatutfallPresentert.stillingsId).isEqualTo(kandidatutfallFåttJobben.stillingsId)
        assertThat(kandidatutfallPresentert.aktørId).isEqualTo(kandidatutfallFåttJobben.aktørId)

        repository.lagreUtfall(
            kandidatutfallPresentert.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(
            kandidatutfallFåttJobben.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(1)
    }

    @Test
    fun `Fått jobben to ganger på samme kandidat og samme kandidatliste skal telles som presentert og fått jobben`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        val kandidatutfall2 = kandidatutfall1.copy()
        assertThat(kandidatutfall1).isEqualTo(kandidatutfall2)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 2))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(1)
    }

    @Test
    fun `Presentert to ganger på samme kandidat og samme kandidatliste skal kun telles som presentert`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT)
        val kandidatutfall2 = kandidatutfall1.copy()
        assertThat(kandidatutfall1).isEqualTo(kandidatutfall2)

        repository.lagreUtfall(kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1)))
        repository.lagreUtfall(kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 2)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 3),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(0)
    }

    @Test
    fun `Fått jobben skal ikke telles hvis det ikke er nyeste registrering`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = PRESENTERT)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 3)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 3),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(0)
    }

    @Test
    fun `Bare siste hendelse per kandidat-i-kandidatliste skal gjelde, uavhengig om flere registreringer har samme timestamp`() {
        val tidspunkt1 = tidspunkt(2020, 1, 2)
        val tidspunkt2 = tidspunkt(2020, 1, 3)
        val kandidatutfalla =
            etKandidatutfall.copy(aktørId = "123", utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt1)
        val kandidatutfallb1 = etKandidatutfall.copy(utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt1)
        val kandidatutfallb2 = etKandidatutfall.copy(utfall = PRESENTERT, tidspunktForHendelsen = tidspunkt2)
        assertThat(kandidatutfallb1.stillingsId).isEqualTo(kandidatutfallb2.stillingsId)
        assertThat(kandidatutfallb1.aktørId).isEqualTo(kandidatutfallb2.aktørId)

        repository.lagreUtfall(kandidatutfalla)
        repository.lagreUtfall(kandidatutfallb1)
        repository.lagreUtfall(kandidatutfallb2)

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 4),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(1)
    }

    @Test
    fun `Statistikk skal også returnere presentert-statistikk for personer i prioritert målgruppe`() {
        val ikkePrioritert = etKandidatutfall.copy(utfall = PRESENTERT)
        val innsatsgruppeIkkeStandard =
            etKandidatutfall.copy(utfall = PRESENTERT, innsatsbehov = BFORM.name, kandidatlisteId = "ikkeStandard")
        assertTrue(erIkkeStandardinnsats(innsatsgruppeIkkeStandard.innsatsbehov!!))
        val under30År =
            etKandidatutfall.copy(utfall = PRESENTERT, innsatsbehov = null, alder = 29, kandidatlisteId = "under30")
        repository.lagreUtfall(innsatsgruppeIkkeStandard, under30År, ikkePrioritert)

        val actual = hentStatistikk(
            fraOgMed = ikkePrioritert.tidspunktForHendelsen,
            tilOgMed = ikkePrioritert.tidspunktForHendelsen,
            navKontor = ikkePrioritert.navKontor
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(3)
        assertThat(actual.antPresentasjoner.under30år).isEqualTo(1)
        assertThat(actual.antPresentasjoner.innsatsgruppeIkkeStandard).isEqualTo(1)
        assertIsZero(actual.antFåttJobben)
    }

    @Test
    fun `Statistikk skal også returnere fått jobben-statistikk for personer i prioritert målgruppe`() {
        val ikkePrioritert = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        val innsatsgruppeIkkeStandard =
            etKandidatutfall.copy(utfall = FATT_JOBBEN, innsatsbehov = BFORM.name, kandidatlisteId = "ikkeStandard")
        assertTrue(erIkkeStandardinnsats(innsatsgruppeIkkeStandard.innsatsbehov!!))
        val under30År =
            etKandidatutfall.copy(utfall = FATT_JOBBEN, innsatsbehov = null, alder = 29, kandidatlisteId = "under30")
        repository.lagreUtfall(innsatsgruppeIkkeStandard, under30År, ikkePrioritert)

        val actual = hentStatistikk(
            fraOgMed = ikkePrioritert.tidspunktForHendelsen,
            tilOgMed = ikkePrioritert.tidspunktForHendelsen,
            navKontor = ikkePrioritert.navKontor
        )

        assertThat(actual.antFåttJobben.innsatsgruppeIkkeStandard).isEqualTo(1)
        assertThat(actual.antFåttJobben.under30år).isEqualTo(1)
        assertThat(actual.antFåttJobben.totalt).isEqualTo(3)
        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
    }

    @Test
    fun `Statistikk skal returnere unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache) {
            expectSuccess = false
        }

        val actual: HttpResponse = uinnloggaClient.get("$basePath/statistikk")
        assertThat(actual.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 1, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
    }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 1 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 2 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        ).run {
            assertThat(antPresentasjoner.totalt).isEqualTo(0)
        }

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(0)

    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antFåttJobben.totalt).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = tidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antFåttJobben.totalt).isEqualTo(1)
    }

    @Test
    fun `Gitt en presentering en gitt dag så skal vi få presentering hvis tilOgMed er samme dag`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = PRESENTERT,
                navKontor = etKontor1,
                tidspunktForHendelsen = tidspunkt(2020, 1, 1).plusHours(13).plusMinutes(55)
            ),
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2019, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 1),
            navKontor = etKontor1
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }

    @Test
    fun `Gitt en presentering en gitt dag så skal vi få presentering hvis fraOgMed er samme dag`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = PRESENTERT,
                navKontor = etKontor1,
                tidspunktForHendelsen = tidspunkt(2020, 1, 1).plusHours(19).plusMinutes(54)
            )
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKontor1
        )

        assertThat(actual.antPresentasjoner.totalt).isEqualTo(1)
    }

    @Test
    fun `antall presentasjoner er ikke det samme som antall personer`() {
        val presentertForStilling1 = etKandidatutfall
        val presentertForStilling2 = presentertForStilling1.copy(
            kandidatlisteId = "kandliste2",
            stillingsId = "stilling2"
        )
        assertThat(presentertForStilling2.tidspunktForHendelsen).isEqualTo(presentertForStilling1.tidspunktForHendelsen)
        repository.lagreUtfall(presentertForStilling1, presentertForStilling2)

        val actual = hentStatistikk(now(), now(), presentertForStilling1.navKontor)

        val antallPersoner = setOf(presentertForStilling1.aktørId, presentertForStilling2.aktørId).size
        assertThat(antallPersoner).isLessThan(actual.antPresentasjoner.totalt)
        assertThat(antallPersoner).isEqualTo(1)
        assertThat(actual.antPresentasjoner.totalt).isEqualTo(2)
    }

    @Test
    fun `antall presentasjoner er ikke det samme som antall personer også for prioritert målgruppe`() {
        val presentertForStilling1 = etKandidatutfall.copy(innsatsbehov = BFORM.name)
        val presentertForStilling2 = presentertForStilling1.copy(
            kandidatlisteId = "kandliste2",
            stillingsId = "stilling2",
            innsatsbehov = IKVAL.name,
            alder = 29
        )
        assertThat(presentertForStilling2.tidspunktForHendelsen).isEqualTo(presentertForStilling1.tidspunktForHendelsen)
        assertTrue(erIkkeStandardinnsats(presentertForStilling1.innsatsbehov!!))
        assertFalse(erIkkeStandardinnsats(presentertForStilling2.innsatsbehov!!))
        repository.lagreUtfall(presentertForStilling1, presentertForStilling2)

        val actual = hentStatistikk(now(), now(), presentertForStilling1.navKontor)

        val antallPersoner = setOf(presentertForStilling1.aktørId, presentertForStilling2.aktørId).size
        assertThat(antallPersoner).isLessThan(actual.antPresentasjoner.totalt)
        assertThat(antallPersoner).isEqualTo(1)
        assertThat(actual.antPresentasjoner.totalt).isEqualTo(2)
    }

    private fun hentStatistikk(
        fraOgMed: ZonedDateTime,
        tilOgMed: ZonedDateTime,
        navKontor: String
    ): StatistikkOutboundDto =
        hentStatistikk(fraOgMed.toLocalDate(), tilOgMed.toLocalDate(), navKontor)

    private fun hentStatistikk(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        navKontor: String
    ): StatistikkOutboundDto = runBlocking {
        client.get("$basePath/statistikk") {
            leggTilQueryParametere(this, fraOgMed, tilOgMed, navKontor)
        }.body()
    }

    @Test
    fun `Kall med token skal få 200 OK`() {
        assertThat(hentStatistikkStatus().status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Kall uten token skal få 401 Unauthorized`() {
        assertThat(hentStatistikkStatus(token = null).status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Kall med utdatert token skal få 401 Unauthorized`() {
        assertThat(hentStatistikkStatus(token = hentToken(mockOAuth2Server, "azuread", expiry = -60)).status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Kall med feil audience skal få 401 Unauthorized`() {
        assertThat(hentStatistikkStatus(token = hentToken(mockOAuth2Server, "azuread", audience = "feilaudience")).status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Kall med feil algoritme skal få 401 Unauthorized`() {
        val token = hentToken(mockOAuth2Server, "azuread").split(".")
        val falskToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.${token[1]}."
        assertThat(hentStatistikkStatus(token = falskToken).status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Kall med feil issuer skal få 401 Unauthorized`() {
        val feilOauthserver = MockOAuth2Server()
        try {
            feilOauthserver.start(port = randomPort())
            assertThat(hentStatistikkStatus(token = hentToken(feilOauthserver, "azuread")).status).isEqualTo(HttpStatusCode.Unauthorized)
        } finally {
            feilOauthserver.shutdown()
        }
    }


    private fun hentStatistikkStatus(
        fraOgMed: LocalDate = LocalDate.of(2020, 10, 1),
        tilOgMed: LocalDate = LocalDate.of(2020, 10, 31),
        navKontor: String = etKandidatutfall.navKontor,
        token: String? = hentToken(mockOAuth2Server, "azuread")
    ) = runBlocking {
        httpKlient().get("${basePath}/statistikk") {
            token?.let {
                header(HttpHeaders.AUTHORIZATION, "Bearer $it")
            }
            leggTilQueryParametere(this, fraOgMed, tilOgMed, navKontor)
        }
    }

    fun httpKlient() = HttpClient(Apache) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
        mockOAuth2Server.shutdown()
    }

    private fun leggTilQueryParametere(
        httpRequestBuilder: HttpRequestBuilder,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        navKontor: String
    ) {
        httpRequestBuilder.url.parameters.apply {
            append(StatistikkParametere.fraOgMed, fraOgMed.toString())
            append(StatistikkParametere.tilOgMed, tilOgMed.toString())
            append(StatistikkParametere.navKontor, navKontor)
        }
    }
}
