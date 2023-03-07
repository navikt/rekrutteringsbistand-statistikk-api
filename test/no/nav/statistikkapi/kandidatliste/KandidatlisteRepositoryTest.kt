package no.nav.statistikkapi.kandidatliste

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.nowOslo
import org.junit.After
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*


class KandidatlisteRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val kandidatlisteRepository = KandidatlisteRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
    }

    @After
    fun afterEach() {
        testRepository.slettAlleKandidatlister()
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

        val antallStillinger = kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste()

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
}