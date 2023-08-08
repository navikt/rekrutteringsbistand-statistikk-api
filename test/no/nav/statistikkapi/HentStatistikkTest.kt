package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
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
import org.junit.After
import org.junit.Test
import java.time.LocalDate

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
    }

    fun lagTidspunkt(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day).atStartOfDay().atOslo()

    @Test
    fun `Siste registrerte presentering på en kandidat og kandidatliste skal telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = PRESENTERT, tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }


    @Test
    fun `Siste registrerte fått jobben på en kandidat og kandidatliste skal telles som presentert og fått jobben`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = FATT_JOBBEN, tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(
                aktørId = "1234",
                utfall = FATT_JOBBEN,
                tidspunktForHendelsen = lagTidspunkt(2020, 10, 15)
            )
        )


        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallFåttJobben).isEqualTo(2)
        assertThat(actual.antallPresentert).isEqualTo(2)
    }


    @Test
    fun `Ikke presentert skal ikke telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = IKKE_PRESENTERT, tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(0)
        assertThat(actual.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Registrert formidling innen tidsperiode skal telles`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Registrert formidling før eller etter gitt tidsperiode skal ikke telles`() {
        repository.lagreUtfall(etKandidatutfall.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1)))
        repository.lagreUtfall(etKandidatutfall.copy(tidspunktForHendelsen = lagTidspunkt(2020, 5, 1)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 2, 1),
            tilOgMed = LocalDate.of(2020, 4, 1),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(0)
        assertThat(actual.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Registrert utfall på samme kandidat på to kandidatlister skal gi to tellinger`() {
        val kandidatutfall1 =
            etKandidatutfall.copy(kandidatlisteId = "1", tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        val kandidatutfall2 =
            kandidatutfall1.copy(kandidatlisteId = "2", tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(kandidatutfall1)
        repository.lagreUtfall(kandidatutfall2)

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(2)
    }

    @Test
    fun `Registrerte utfall på to kandidater på en kandidatliste skal gi to tellinger`() {
        val kandidatutfall1 = etKandidatutfall.copy(aktørId = "1", tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        val kandidatutfall2 = kandidatutfall1.copy(aktørId = "2", tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)

        repository.lagreUtfall(kandidatutfall1)
        repository.lagreUtfall(kandidatutfall2)

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(2)
    }

    @Test
    fun `Presentert og fått jobben på samme kandidat, samme kandidatliste og samme tidspunkt skal telles som presentert og fått jobben`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(1)
    }

    @Test
    fun `Fått jobben to ganger på samme kandidat og samme kandidatliste skal telles som presentert og fått jobben`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        val kandidatutfall2 = kandidatutfall1.copy()
        assertThat(kandidatutfall1).isEqualTo(kandidatutfall2)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 2))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(1)
    }

    @Test
    fun `Presentert to ganger på samme kandidat og samme kandidatliste skal kun telles som presentert`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT)
        val kandidatutfall2 = kandidatutfall1.copy()
        assertThat(kandidatutfall1).isEqualTo(kandidatutfall2)

        repository.lagreUtfall(kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1)))
        repository.lagreUtfall(kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 2)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 3),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Fått jobben skal ikke telles hvis det ikke er nyeste registrering`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = PRESENTERT)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 1))
        )
        repository.lagreUtfall(kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 3)))

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 3),
            navKontor = etKandidatutfall.navKontor
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
        assertThat(actual.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Statistikk skal også returnere presentert-statistikk for personer i prioritert målgruppe`() {
        val tidspunkt = lagTidspunkt(2023, 8, 1)
        val presentertPrioritert1 = etKandidatutfallIPrioritertMålgruppeMedUkjentHullICv.copy(
            aktørId = "123", utfall = PRESENTERT, tidspunktForHendelsen = tidspunkt
        )
        val presentertPrioritert2 = etKandidatutfallIPrioritertMålgruppeMedUkjentHullICv.copy(
            aktørId = "456", utfall = PRESENTERT, tidspunktForHendelsen = tidspunkt
        )
        val presentertIkkePrioritert = etKandidatutfallIkkeIPrioritertMålgruppe.copy(
            aktørId = "789", utfall = PRESENTERT, tidspunktForHendelsen = tidspunkt
        )

        repository.lagreUtfall(presentertPrioritert1)
        repository.lagreUtfall(presentertPrioritert2)
        repository.lagreUtfall(presentertIkkePrioritert)

        val statistikk = hentStatistikk(
            fraOgMed = tidspunkt.minusDays(1).toLocalDate(),
            tilOgMed = tidspunkt.plusDays(1).toLocalDate(),
            navKontor = presentertPrioritert1.navKontor
        )

        assertThat(statistikk.antallPresentert).isEqualTo(3)
        assertThat(statistikk.antallPresentertIPrioritertMålgruppe).isEqualTo(2)
    }

    @Test
    fun `Statistikk skal også returnere fått jobben-statistikk for personer i prioritert målgruppe`() {
        val tidspunkt = lagTidspunkt(2023, 8, 8)
        val fåttJobbenPrioritert1 = etKandidatutfallIPrioritertMålgruppeMedUkjentHullICv.copy(
            aktørId = "123", utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt
        )
        val fåttJobbenPrioritert2 = etKandidatutfallIPrioritertMålgruppeMedUkjentHullICv.copy(
            aktørId = "456", utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt
        )
        val fåttJobbenIkkePrioritert = etKandidatutfallIkkeIPrioritertMålgruppe.copy(
            aktørId = "789", utfall = FATT_JOBBEN, tidspunktForHendelsen = tidspunkt
        )

        repository.lagreUtfall(fåttJobbenPrioritert1)
        repository.lagreUtfall(fåttJobbenPrioritert2)
        repository.lagreUtfall(fåttJobbenIkkePrioritert)

        val statistikk = hentStatistikk(
            fraOgMed = tidspunkt.minusDays(1).toLocalDate(),
            tilOgMed = tidspunkt.plusDays(1).toLocalDate(),
            navKontor = fåttJobbenPrioritert1.navKontor
        )

        assertThat(statistikk.antallFåttJobben).isEqualTo(3)
        assertThat(statistikk.antallFåttJobbenIPrioritertMålgruppe).isEqualTo(2)
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
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 1, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antallPresentert).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antallPresentert).isEqualTo(0)
    }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antallPresentert).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        ).run {
            assertThat(antallPresentert).isEqualTo(0)
        }

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antallPresentert).isEqualTo(1)

    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 1 være 0`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = kandidatutfall1.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor1
        )

        assertThat(actual.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 2 være 1`() {
        val kandidatutfall1 = etKandidatutfall.copy(utfall = PRESENTERT, navKontor = etKontor1)
        val kandidatutfall2 = etKandidatutfall.copy(utfall = FATT_JOBBEN, navKontor = etKontor2)
        assertThat(kandidatutfall1.stillingsId).isEqualTo(kandidatutfall2.stillingsId)
        assertThat(kandidatutfall1.aktørId).isEqualTo(kandidatutfall2.aktørId)

        repository.lagreUtfall(
            kandidatutfall1.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 15))
        )

        repository.lagreUtfall(
            kandidatutfall2.copy(tidspunktForHendelsen = lagTidspunkt(2020, 10, 16))
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 10, 1),
            tilOgMed = LocalDate.of(2020, 10, 31),
            navKontor = etKontor2
        )

        assertThat(actual.antallFåttJobben).isEqualTo(1)
    }

    @Test
    fun `Gitt en presentering en gitt dag så skal vi få presentering hvis tilOgMed er samme dag`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = PRESENTERT,
                navKontor = etKontor1,
                tidspunktForHendelsen = lagTidspunkt(2020, 1, 1).plusHours(13).plusMinutes(55)
            ),
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2019, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 1),
            navKontor = etKontor1
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Gitt en presentering en gitt dag så skal vi få presentering hvis fraOgMed er samme dag`() {
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = PRESENTERT,
                navKontor = etKontor1,
                tidspunktForHendelsen = lagTidspunkt(2020, 1, 1).plusHours(19).plusMinutes(54)
            )
        )

        val actual = hentStatistikk(
            fraOgMed = LocalDate.of(2020, 1, 1),
            tilOgMed = LocalDate.of(2020, 1, 2),
            navKontor = etKontor1
        )

        assertThat(actual.antallPresentert).isEqualTo(1)
    }

    private fun hentStatistikk(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        navKontor: String
    ): StatistikkOutboundDto = runBlocking {
        client.get("$basePath/statistikk") {
            leggTilQueryParametere(this, fraOgMed, tilOgMed, navKontor)
        }.body()
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
