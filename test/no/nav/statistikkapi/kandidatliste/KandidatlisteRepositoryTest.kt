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
    fun `Tell antall kandidatlister for direktemeldte stillinger`() {
        val opprettetKandidatlistehendelse = lagOpprettetKandidatlisteHendelse(stillingOpprettetTidspunkt = null, erDirektemeldt = true)
        val oppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
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
            lagOpprettetKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, stillingOpprettetTidspunkt = null, erDirektemeldt = true)
        val oppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Skal telle kandidatliste for opprettet stilling kun én gang per kandidatlisteId`() {
        val kandidatlisteId = UUID.randomUUID()
        val opprettetKandidatlistehendelse =
            lagOpprettetKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, stillingOpprettetTidspunkt = null, erDirektemeldt = true)
        val oppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        val nyOppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteId, erDirektemeldt = true)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger skal kun telle kandidatlister tilknyttet opprettede stillinger`() {
        val kandidatlisteIdDirektemeldt = UUID.randomUUID()
        val opprettetKandidatlisteHendelseDirektemeldt = lagOpprettetKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true, stillingOpprettetTidspunkt = null)
        val oppdatertKandidatlistehendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true)
        val nyOpprettetKandidatlisteHendelseDirektemeldt = lagOpprettetKandidatlisteHendelse(erDirektemeldt = true, stillingOpprettetTidspunkt = null)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(nyOpprettetKandidatlisteHendelseDirektemeldt)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger skal ikke telle med kandidatlister tilknyttet eksterne stillinger`() {
        val kandidatlisteIdDirektemeldt = UUID.randomUUID()
        val opprettetKandidatlisteHendelseDirektemeldt = lagOpprettetKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true, stillingOpprettetTidspunkt = null)
        val oppdatertKandidatlistehendelseDirektemeldt = lagOppdatertKandidatlisteHendelse(kandidatlisteId = kandidatlisteIdDirektemeldt, erDirektemeldt = true)
        val oppdatertKandidatlistehendelseEkstern = lagOppdatertKandidatlisteHendelse(erDirektemeldt = false)
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlisteHendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseDirektemeldt)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelseEkstern)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet eksterne stillinger`() {
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
    ): Kandidatlistehendelse {
        return Kandidatlistehendelse(
            stillingOpprettetTidspunkt = nowOslo(),
            stillingensPubliseringstidspunkt = nowOslo(),
            organisasjonsnummer = "123123123",
            antallStillinger = 40,
            antallKandidater = 20,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId.toString(),
            tidspunkt = nowOslo(),
            stillingsId = UUID.randomUUID().toString(),
            utførtAvNavIdent = "A100100",
            eventName = oppdaterteKandidatlisteEventName
        )
    }
}