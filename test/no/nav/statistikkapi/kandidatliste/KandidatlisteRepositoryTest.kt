package no.nav.statistikkapi.kandidatliste

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.etKandidatutfallIkkeIPrioritertMålgruppe
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*


class KandidatlisteRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val kandidatlisteRepository = KandidatlisteRepository(database.dataSource)
        private val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
        private val stillingRepository = StillingRepository(database.dataSource)
    }

    @After
    fun afterEach() {
        testRepository.slettAlleKandidatlister()
        testRepository.slettAlleStillinger()
        testRepository.slettAlleUtfall()
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger`() {
        val opprettetKandidatlistehendelse =
            lagOpprettetKandidatlisteHendelse(stillingOpprettetTidspunkt = null, erDirektemeldt = true)
        val oppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet stilling per måned`() {
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = false))

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned()

        assertThat(antallKandidatlister.values.contains(2))
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldt stilling per måned`() {
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = false))

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned()

        assertThat(antallKandidatlister.values.contains(1))
    }

    @Test
    fun `Skal kunne telle svært mange kandidatlister for direktemeldte stillinger`() {
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))
        kandidatlisteRepository.lagreKandidatlistehendelse(lagOppdatertKandidatlisteHendelse(erDirektemeldt = true))

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()

        assertThat(antallKandidatlister).isEqualTo(20)
    }

    @Test
    fun `Tell antall kandidatlister for opprettede stillinger skal telle for både eksterne og direktemeldte stillinger`() {
        val oppdatertKandidatlistehendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        val oppdatertKandidatlistehendelseEkstern = lagOppdatertKandidatlisteHendelse(erDirektemeldt = false)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseEkstern)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(2)
    }

    @Test
    fun `Skal telle kandidatliste for opprettet stilling selv om stillingOpprettetTidspunkt var null i en tidligere rad i databasen`() {
        val kandidatlisteId = UUID.randomUUID()
        val opprettetKandidatlistehendelse =
            lagOpprettetKandidatlisteHendelse(
                kandidatlisteId = kandidatlisteId,
                stillingOpprettetTidspunkt = null,
                erDirektemeldt = true
            )
        val oppdatertKandidatlistehendelse =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Skal telle kandidatliste for opprettet stilling kun én gang per kandidatlisteId`() {
        val kandidatlisteId = UUID.randomUUID()
        val opprettetKandidatlistehendelse =
            lagOpprettetKandidatlisteHendelse(
                kandidatlisteId = kandidatlisteId,
                stillingOpprettetTidspunkt = null,
                erDirektemeldt = true
            )
        val oppdatertKandidatlistehendelse =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        val nyOppdatertKandidatlistehendelse =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger skal kun telle kandidatlister tilknyttet opprettede stillinger`() {
        val kandidatlisteIdDirektemeldt = UUID.randomUUID()
        val opprettetKandidatlisteHendelseDirektemeldt = lagOpprettetKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdDirektemeldt,
            erDirektemeldt = true,
            stillingOpprettetTidspunkt = null
        )
        val oppdatertKandidatlistehendelseDirektemeldt =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true)
        val nyOpprettetKandidatlisteHendelseDirektemeldt =
            lagOpprettetKandidatlisteHendelse(erDirektemeldt = true, stillingOpprettetTidspunkt = null)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOpprettetKandidatlisteHendelseDirektemeldt)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger skal ikke telle med kandidatlister tilknyttet eksterne stillinger`() {
        val kandidatlisteIdDirektemeldt = UUID.randomUUID()
        val opprettetKandidatlisteHendelseDirektemeldt = lagOpprettetKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdDirektemeldt,
            erDirektemeldt = true,
            stillingOpprettetTidspunkt = null
        )
        val oppdatertKandidatlistehendelseDirektemeldt =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true)
        val oppdatertKandidatlistehendelseEkstern = lagOppdatertKandidatlisteHendelse(erDirektemeldt = false)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseEkstern)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet eksterne stillinger skal kun telle kandidatlister tilknyttet opprettede stillinger`() {
        val opprettetKandidatlisteHendelse =
            lagOpprettetKandidatlisteHendelse(erDirektemeldt = false, stillingOpprettetTidspunkt = null)
        val nyOpprettetKandidatlisteHendelse = lagOpprettetKandidatlisteHendelse(erDirektemeldt = false)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOpprettetKandidatlisteHendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet eksterne stillinger skal ikke telle kandidatlister tilknyttet direktemeldte stillinger`() {
        val oppdatertKandidatlisteHendelseEkstern = lagOppdatertKandidatlisteHendelse(erDirektemeldt = false)
        val oppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseDirektemeldt)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Skal telle kandidatliste tilknyttet ekstern stilling kun en gang per kandidatlisteId`() {
        val kandidatlisteId = UUID.randomUUID()
        val opprettetKandidatlisteHendelse =
            lagOpprettetKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = false)
        val oppdatertKandidatliseHendelse =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = false)
        val nyOppdatertKandidatlisteHendelse =
            lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = false)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatliseHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlisteHendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Skal returnere totale antall stillinger for alle eksterne stillingsannonser med kandidatliste`() {
        val opprettetKandidatlisteHendelseEkstern = lagOpprettetKandidatlisteHendelse(erDirektemeldt = false)
        val nyOpprettetKandidatlisteHendelseEkstern = lagOpprettetKandidatlisteHendelse(erDirektemeldt = false)
        val oppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOpprettetKandidatlisteHendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseDirektemeldt)

        val antallStillinger =
            kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste()

        assertThat(antallStillinger).isEqualTo(80)
    }

    @Test
    fun `Skal returnere totale antall stillinger for alle direktemeldte stillingsannonser`() {
        val opprettetKandidatlisteHendelseEkstern = lagOpprettetKandidatlisteHendelse(erDirektemeldt = false)
        val oppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        val nyOppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlisteHendelseDirektemeldt)

        val antallStillinger = kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser()

        assertThat(antallStillinger).isEqualTo(80)
    }

    @Test
    fun `Skal returnere totale antall stillinger for alle stillingsannonser med kandidatliste`() {
        val opprettetKandidatlisteHendelseEkstern = lagOpprettetKandidatlisteHendelse(erDirektemeldt = false)
        val oppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        val nyOppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlisteHendelseDirektemeldt)

        val antallStillinger = kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste()

        assertThat(antallStillinger).isEqualTo(120)
    }

    @Test
    fun `Skal telle antall stillinger fra siste rad for en kandidatliste tilknyttet direktemeldt stilling`() {
        val kandidatlisteId = UUID.randomUUID()
        val oppdatertKandidatlisteHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        )
        val nyOppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 2
        )

        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlistehendelse)

        val antallStillinger = kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser()

        assertThat(antallStillinger).isEqualTo(2)
    }

    @Test
    fun `Skal telle antall stillinger fra siste rad for en kandidatliste tilknyttet ekstern stilling`() {
        val kandidatlisteId = UUID.randomUUID()
        val oppdatertKandidatlisteHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        )
        val nyOppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false,
            antallStillinger = 2
        )

        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlistehendelse)

        val antallStillinger =
            kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste()

        assertThat(antallStillinger).isEqualTo(2)
    }

    @Test
    fun `Skal telle antall stillinger fra siste rad for en kandidatliste tilknyttet stilling`() {
        val kandidatlisteIdDirektemeldt = UUID.randomUUID()
        val kandidatlisteIdEkstern = UUID.randomUUID()
        val oppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdDirektemeldt,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        )
        val nyOppdatertKandidatlisteHendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdDirektemeldt,
            erDirektemeldt = true,
            antallStillinger = 2
        )
        val oppdatertKandidatlistehendelseEkstern = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdEkstern,
            erDirektemeldt = false,
            antallStillinger = 20,
            tidspunkt = nowOslo().minusNanos(1)
        )

        val nyOppdatertKandidatlistehendelseEkstern = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteIdEkstern,
            erDirektemeldt = false,
            antallStillinger = 4
        )

        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseEkstern)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlistehendelseEkstern)

        val antallStillinger = kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste()

        assertThat(antallStillinger).isEqualTo(6)
    }

    @Test
    fun `Skal telle antall kandidatlister tilknyttet direktemeldte stillinger som har blitt opprettet hvor minst en kandidat har blitt presentert`() {
        val kandidatlisteId = UUID.randomUUID()
        val nyKandidatlisteId = UUID.randomUUID()

        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }

        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = nyKandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 2
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = null
            )
        }

        uniktKandidatutfall(kandidatlisteId.toString()).also {
            kandidatutfallRepository.lagreUtfall(it)
        }
        uniktKandidatutfall(nyKandidatlisteId.toString()).also {
            kandidatutfallRepository.lagreUtfall(it)
        }
        uniktKandidatutfall(nyKandidatlisteId.toString()).also {
            it.copy(aktørId = "10108000398")
            kandidatutfallRepository.lagreUtfall(it)
        }

        val antallKandidatlisterMedPresentertKandidat =
            kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()

        assertThat(antallKandidatlisterMedPresentertKandidat).isEqualTo(2)
    }

    @Test
    fun `Skal ikke telle kandidatliste der vi ikke har stillingsinformasjon når vi teller kandidatlister med minst en presentert kandidat`() {
        val kandidatlisteId = UUID.randomUUID()
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
        }

        uniktKandidatutfall(kandidatlisteId.toString()).also {
            kandidatutfallRepository.lagreUtfall(it)
        }

        val antallKandidatlisterMedPresentertKandidat =
            kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()

        assertThat(antallKandidatlisterMedPresentertKandidat).isEqualTo(0)
    }

    @Test
    fun `Skal ikke telle kandidatliste tilknyttet en stilling som ikke har stillingskategori STILLING eller null`() {
        val kandidatlisteId = UUID.randomUUID()
        val nyKandidatlisteId = UUID.randomUUID()
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.JOBBMESSE
            )
        }
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = nyKandidatlisteId,
            erDirektemeldt = true,
            antallStillinger = 40,
            tidspunkt = nowOslo().minusNanos(1)
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.FORMIDLING
            )
        }

        uniktKandidatutfall(kandidatlisteId.toString()).also {
            kandidatutfallRepository.lagreUtfall(it)
        }
        uniktKandidatutfall(nyKandidatlisteId.toString()).also {
            kandidatutfallRepository.lagreUtfall(it)
        }

        val antallKandidatlisterMedPresentertKandidat =
            kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()

        assertThat(antallKandidatlisterMedPresentertKandidat).isEqualTo(0)
    }

    @Test
    fun `Skal kunne telle antall kandidatlister der minst én kandidat i prioritert målgruppe fikk jobben`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()

        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )

        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = annenKandidatlisteId,
            erDirektemeldt = true
        )

        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)

        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )

        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = null
        )

        uniktKandidatutfall(kandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfall(annenKandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }

        val antall = kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()

        assertThat(antall).isEqualTo(2)
    }

    @Test
    fun `Skal ikke inkludere kandidater som kun er blitt presentert fra telling for antall kandidatlister der minst én kandidat i prioritert målgruppe fikk jobben`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()

        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )

        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = annenKandidatlisteId,
            erDirektemeldt = true
        )

        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)

        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )

        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = null
        )

        uniktKandidatutfall(kandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfall(annenKandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }

        val antall = kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Telling for antall kandidatlister der minst én kandidat i prioritert målgruppe fikk jobben gjelder ikke for formidlingsstillinger`() {
        val kandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.FORMIDLING
        )
        uniktKandidatutfall(kandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }

        val antall = kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Telling for antall kandidatlister der minst én kandidat i prioritert målgruppe fikk jobben gjelder ikke når kandidaten som fikk jobben senere ble satt tilbake til presentert`() {
        val kandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        val fåttJobbenUtfall = uniktKandidatutfall(kandidatlisteId.toString()).copy(
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = nowOslo().minusDays(1)
        )
        val presentertUtfall = fåttJobbenUtfall.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo())
        kandidatutfallRepository.lagreUtfall(fåttJobbenUtfall)
        kandidatutfallRepository.lagreUtfall(presentertUtfall)

        val antall = kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal telle antall direktemeledte stillinger med tom kandidatliste`() {
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = UUID.randomUUID(),
            erDirektemeldt = true
        ).copy(
            antallKandidater = 0
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }

        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = UUID.randomUUID(),
            erDirektemeldt = true
        ).copy(
            antallKandidater = 2
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }

        val antall = kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal kunne telle flere antall direktemeledte stillinger med tom kandidatliste`() {
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = UUID.randomUUID(),
            erDirektemeldt = true
        ).copy(
            antallKandidater = 0
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = UUID.randomUUID(),
            erDirektemeldt = true
        ).copy(
            antallKandidater = 0
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = UUID.randomUUID(),
            erDirektemeldt = true
        ).copy(
            antallKandidater = 2
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
            stillingRepository.lagreStilling(
                stillingsuuid = it.stillingsId,
                stillingskategori = Stillingskategori.STILLING
            )
        }

        val antall = kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste()

        assertThat(antall).isEqualTo(2)
    }

    @Test
    fun `Skal se på antall kandidater i siste kandidatlistemelding når man telle antall direktemeldte stillinger med tom kandidatliste`() {
        val kandidatlisteId = UUID.randomUUID()
        val stillingsId = UUID.randomUUID().toString()
        stillingRepository.lagreStilling(
            stillingsuuid = stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true
        ).copy(
            antallKandidater = 0,
            stillingsId = stillingsId
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
        }
        lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true
        ).copy(
            antallKandidater = 2,
            stillingsId = stillingsId
        ).also {
            kandidatlisteRepository.lagreKandidatlistehendelse(it)
        }

        val antall = kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal returnere unike antall arbeidsgivere som har fått opprettet direktemeldt stilling`() {
        val hendelseDirektemeldtStilling1 = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true).copy(organisasjonsnummer = "1234")
        val hendelseDirektemeldtStilling2 = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true).copy(organisasjonsnummer = "2345")
        val hendelseEksternStilling = lagOppdatertKandidatlisteHendelse(erDirektemeldt = false).copy(organisasjonsnummer = "3456")
        val hendelseDirektemeldtStillingIkkePublisert = lagOpprettetKandidatlisteHendelse(erDirektemeldt = true, stillingOpprettetTidspunkt = null).copy(organisasjonsnummer = "4567")
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelseDirektemeldtStilling1)
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelseDirektemeldtStilling2)
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelseEksternStilling)
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelseDirektemeldtStillingIkkePublisert)

        val antall = kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger()

        assertThat(antall).isEqualTo(2)
    }

    @Test
    fun `Skal kunne telle antall kandidatlister tilknyttet direktemeldt stilling der minst én kandidat fikk jobben`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = annenKandidatlisteId,
            erDirektemeldt = true
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = null
        )
        uniktKandidatutfallIkkeIPrioritertMålgruppe(kandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfall(annenKandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }

        val antall = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal ikke inkludere kandidater som kun er blitt presentert fra telling for antall kandidatlister tilknyttet direktemeldt stilling der minst én kandidat fikk jobben`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()
        val tredjeKandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = annenKandidatlisteId,
            erDirektemeldt = true
        )
        val tredjeHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = tredjeKandidatlisteId,
            erDirektemeldt = true
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(tredjeHendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = null
        )
        stillingRepository.lagreStilling(
            stillingsuuid = tredjeHendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        uniktKandidatutfallIkkeIPrioritertMålgruppe(kandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfallIkkeIPrioritertMålgruppe(annenKandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfall(annenKandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfallIkkeIPrioritertMålgruppe(tredjeKandidatlisteId.toString()).copy(utfall = Utfall.PRESENTERT)
            .also { kandidatutfallRepository.lagreUtfall(it) }

        val antall = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Telling for antall kandidatlister tilknyttet direktemeldt stilling der minst én kandidat fikk jobben gjelder ikke for formidlingsstillinger`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = true
        )
        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.FORMIDLING
        )
        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = Stillingskategori.FORMIDLING
        )
        uniktKandidatutfallIkkeIPrioritertMålgruppe(kandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfall(kandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)
            .also { kandidatutfallRepository.lagreUtfall(it) }
        uniktKandidatutfallIkkeIPrioritertMålgruppe(annenKandidatlisteId.toString()).copy(utfall = Utfall.FATT_JOBBEN)

        val antall = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Telling for antall kandidatlister tilknyttet direktemeldt stilling der minst én kandidat fikk jobben gjelder ikke når kandidaten som fikk jobben senere ble satt tilbake til presentert`() {
        val kandidatlisteId = UUID.randomUUID()
        val annenKandidatlisteId = UUID.randomUUID()
        val hendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = kandidatlisteId,
            erDirektemeldt = false
        )
        val annenHendelse = lagOppdatertKandidatlisteHendelse(
            kandidatlisteId = annenKandidatlisteId,
            erDirektemeldt = true
        )
        kandidatlisteRepository.lagreKandidatlistehendelse(hendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(annenHendelse)
        stillingRepository.lagreStilling(
            stillingsuuid = hendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        stillingRepository.lagreStilling(
            stillingsuuid = annenHendelse.stillingsId,
            stillingskategori = Stillingskategori.STILLING
        )
        val fåttJobbenUtfall = uniktKandidatutfallIkkeIPrioritertMålgruppe(kandidatlisteId.toString()).copy(
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = nowOslo().minusDays(1)
        )
        val annenFåttJobbenUtfalll = uniktKandidatutfallIkkeIPrioritertMålgruppe(annenKandidatlisteId.toString()).copy(
            utfall = Utfall.FATT_JOBBEN,
            tidspunktForHendelsen = nowOslo().minusDays(1)
        )
        val presentertUtfall = fåttJobbenUtfall.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo())
        val annenPresentertUtfall = annenFåttJobbenUtfalll.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo())
        kandidatutfallRepository.lagreUtfall(fåttJobbenUtfall)
        kandidatutfallRepository.lagreUtfall(annenFåttJobbenUtfalll)
        kandidatutfallRepository.lagreUtfall(presentertUtfall)
        kandidatutfallRepository.lagreUtfall(annenPresentertUtfall)

        val antall = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()

        assertThat(antall).isEqualTo(0)
    }

    fun lagOpprettetKandidatlisteHendelse(
        kandidatlisteId: UUID = UUID.randomUUID(),
        erDirektemeldt: Boolean,
        stillingOpprettetTidspunkt: ZonedDateTime? = nowOslo()
    ): Kandidatlistehendelse {
        return Kandidatlistehendelse(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            stillingensPubliseringstidspunkt = nowOslo(),
            organisasjonsnummer = "123123123",
            antallStillinger = 40,
            antallKandidater = 20,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId.toString(),
            tidspunkt = nowOslo(),
            stillingsId = UUID.randomUUID().toString(),
            utførtAvNavIdent = "A100100",
            eventName = opprettetKandidatlisteEventName
        )
    }

    fun lagOppdatertKandidatlisteHendelse(
        kandidatlisteId: UUID = UUID.randomUUID(),
        erDirektemeldt: Boolean,
        antallStillinger: Int = 40,
        tidspunkt: ZonedDateTime = nowOslo()
    ): Kandidatlistehendelse {
        return Kandidatlistehendelse(
            stillingOpprettetTidspunkt = nowOslo(),
            stillingensPubliseringstidspunkt = nowOslo(),
            organisasjonsnummer = "123123123",
            antallStillinger = antallStillinger,
            antallKandidater = 20,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId.toString(),
            tidspunkt = tidspunkt,
            stillingsId = UUID.randomUUID().toString(),
            utførtAvNavIdent = "A100100",
            eventName = oppdaterteKandidatlisteEventName
        )
    }

    private fun uniktKandidatutfall(kandidatlisteId: String = "385c74d1-0d14-48d7-9a9b-b219beff22c8") =
        etKandidatutfall.copy(
            stillingsId = UUID.randomUUID().toString(),
            aktørId = UUID.randomUUID().toString().substring(26, 35),
            kandidatlisteId = kandidatlisteId
        )

    private fun uniktKandidatutfallIkkeIPrioritertMålgruppe(kandidatlisteId: String = "385c74d1-0d14-48d7-9a9b-b219beff22c8") =
        etKandidatutfallIkkeIPrioritertMålgruppe.copy(
            stillingsId = UUID.randomUUID().toString(),
            aktørId = UUID.randomUUID().toString().substring(26, 35),
            kandidatlisteId = kandidatlisteId
        )
}