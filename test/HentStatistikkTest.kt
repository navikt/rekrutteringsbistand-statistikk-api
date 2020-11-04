import assertk.assertThat
import assertk.assertions.isEqualTo
import db.TestDatabase
import db.TestRepository
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.rekrutteringsbistand.statistikk.StatistikkInboundDto
import no.nav.rekrutteringsbistand.statistikk.StatistikkOutboundDto
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.Utfall.*
import org.junit.After
import org.junit.Test
import java.time.LocalDate

@KtorExperimentalAPI
class HentStatistikkTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabase()
        private val repository = Repository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
        private val port = randomPort()

        init {
            start(database, port)
        }
    }

    @Test
    fun `Siste registrerte presentering på en kandidat og kandidatliste skal telles`() = runBlocking {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = PRESENTERT.name),
            LocalDate.of(2020, 10, 15).atStartOfDay()
        )

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Siste registrerte fått jobben på en kandidat og kandidatliste skal telles som presentert og fått jobben`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKandidatutfall.navKontor
                )
            }

            assertThat(response.antallFåttJobben).isEqualTo(1)
            assertThat(response.antallPresentert).isEqualTo(1)
        }

    @Test
    fun `Ikke presentert skal ikke telles`() = runBlocking {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = IKKE_PRESENTERT.name),
            LocalDate.of(2020, 10, 15).atStartOfDay()
        )

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(0)
        assertThat(response.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Registrert formidling innen tidsperiode skal telles`() = runBlocking {
        repository.lagreUtfall(etKandidatutfall, LocalDate.of(2020, 10, 15).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(1)
    }

    @Test
    fun `Registrert formidling før eller etter gitt tidsperiode skal ikke telles`() = runBlocking {
        repository.lagreUtfall(etKandidatutfall, LocalDate.of(2020, 1, 1).atStartOfDay())
        repository.lagreUtfall(etKandidatutfall, LocalDate.of(2021, 5, 1).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 2, 1),
                tilOgMed = LocalDate.of(2020, 4, 1),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(0)
        assertThat(response.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Registrert utfall på samme kandidat på to kandidatlister skal gi to tellinger`() = runBlocking {
        repository.lagreUtfall(etKandidatutfall.copy(kandidatlisteId = "1"), LocalDate.of(2020, 1, 1).atStartOfDay())
        repository.lagreUtfall(etKandidatutfall.copy(kandidatlisteId = "2"), LocalDate.of(2020, 1, 1).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 1, 1),
                tilOgMed = LocalDate.of(2020, 1, 2),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(2)
    }

    @Test
    fun `Registrerte utfall på to kandidater på en kandidatliste skal gi to tellinger`() = runBlocking {
        repository.lagreUtfall(etKandidatutfall.copy(aktørId = "1"), LocalDate.of(2020, 1, 1).atStartOfDay())
        repository.lagreUtfall(etKandidatutfall.copy(aktørId = "2"), LocalDate.of(2020, 1, 1).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 1, 1),
                tilOgMed = LocalDate.of(2020, 1, 2),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(2)
    }

    @Test
    fun `Presentert og fått jobben på samme kandidat og samme kandidatliste skal telles som presentert og fått jobben`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name),
                LocalDate.of(2020, 1, 1).atStartOfDay()
            )
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name),
                LocalDate.of(2020, 1, 1).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 1, 1),
                    tilOgMed = LocalDate.of(2020, 1, 2),
                    navKontor = etKandidatutfall.navKontor
                )
            }

            assertThat(response.antallPresentert).isEqualTo(1)
            assertThat(response.antallFåttJobben).isEqualTo(1)
        }

    @Test
    fun `Fått jobben to ganger på samme kandidat og samme kandidatliste skal telles som presentert og fått jobben`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name),
                LocalDate.of(2020, 1, 1).atStartOfDay()
            )
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name),
                LocalDate.of(2020, 1, 2).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 1, 1),
                    tilOgMed = LocalDate.of(2020, 1, 2),
                    navKontor = etKandidatutfall.navKontor
                )
            }

            assertThat(response.antallPresentert).isEqualTo(1)
            assertThat(response.antallFåttJobben).isEqualTo(1)
        }

    @Test
    fun `Presentert to ganger på samme kandidat og samme kandidatliste skal kun telles som presentert`() = runBlocking {
        repository.lagreUtfall(etKandidatutfall.copy(utfall = PRESENTERT.name), LocalDate.of(2020, 1, 1).atStartOfDay())
        repository.lagreUtfall(etKandidatutfall.copy(utfall = PRESENTERT.name), LocalDate.of(2020, 1, 2).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 1, 1),
                tilOgMed = LocalDate.of(2020, 1, 3),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(1)
        assertThat(response.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Fått jobben skal ikke telles hvis det ikke er nyeste registrering`() = runBlocking {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = FATT_JOBBEN.name),
            LocalDate.of(2020, 1, 1).atStartOfDay()
        )
        repository.lagreUtfall(etKandidatutfall.copy(utfall = PRESENTERT.name), LocalDate.of(2020, 1, 2).atStartOfDay())

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 1, 1),
                tilOgMed = LocalDate.of(2020, 1, 3),
                navKontor = etKandidatutfall.navKontor
            )
        }

        assertThat(response.antallPresentert).isEqualTo(1)
        assertThat(response.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Statistikk skal returnere unauthorized hvis man ikke er logget inn`() = runBlocking {
        val uinnloggaClient = HttpClient(Apache)
        val response: HttpResponse = uinnloggaClient.get("$basePath/statistikk")
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 1 være 0`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor1
                )
            }

            assertThat(response.antallPresentert).isEqualTo(0)
        }

    @Test
    fun `Gitt presentert med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 1`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor2
                )
            }

            assertThat(response.antallPresentert).isEqualTo(1)
        }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 1 være 0`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor1
                )
            }

            assertThat(response.antallPresentert).isEqualTo(0)
        }

    @Test
    fun `Gitt fått jobb med kontor 1 og deretter med kontor 2 så skal antall presentert for kontor 2 være 1`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor2
                )
            }

            assertThat(response.antallPresentert).isEqualTo(1)
        }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 1 være 0 (Er dette riktig?)`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor1
                )
            }

            assertThat(response.antallPresentert).isEqualTo(0)
        }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall presentert for kontor 2 være 1 (Er dette riktig?)`() =
        runBlocking {
            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
                LocalDate.of(2020, 10, 15).atStartOfDay()
            )

            repository.lagreUtfall(
                etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
                LocalDate.of(2020, 10, 16).atStartOfDay()
            )

            val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
                body = StatistikkInboundDto(
                    fraOgMed = LocalDate.of(2020, 10, 1),
                    tilOgMed = LocalDate.of(2020, 10, 31),
                    navKontor = etKontor2
                )
            }

            assertThat(response.antallPresentert).isEqualTo(1)
        }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 1 være 0`() = runBlocking {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
            LocalDate.of(2020, 10, 15).atStartOfDay()
        )

        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
            LocalDate.of(2020, 10, 16).atStartOfDay()
        )

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31),
                navKontor = etKontor1
            )
        }

        assertThat(response.antallFåttJobben).isEqualTo(0)
    }

    @Test
    fun `Gitt presentert med kontor 1 og deretter fått jobb med kontor 2 så skal antall fått jobb for kontor 2 være 1`() = runBlocking {
        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = PRESENTERT.name, navKontor = etKontor1),
            LocalDate.of(2020, 10, 15).atStartOfDay()
        )

        repository.lagreUtfall(
            etKandidatutfall.copy(utfall = FATT_JOBBEN.name, navKontor = etKontor2),
            LocalDate.of(2020, 10, 16).atStartOfDay()
        )

        val response: StatistikkOutboundDto = client.get("$basePath/statistikk") {
            body = StatistikkInboundDto(
                fraOgMed = LocalDate.of(2020, 10, 1),
                tilOgMed = LocalDate.of(2020, 10, 31),
                navKontor = etKontor2
            )
        }

        assertThat(response.antallFåttJobben).isEqualTo(1)
    }


    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
