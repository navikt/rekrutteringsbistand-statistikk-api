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
    fun `Tell antall kandidatlister for opprettede stillinger`() {
        val opprettetKandidatlistehendelse = lagOpprettetKandidatlisteHendelse(stillingOpprettetTidspunkt = null)
        val oppdatertKandidatlistehendelse = lagOppdatertKandidatlisteHendelse()
        kandidatlisteRepository.lagreKandidatlistehendelse(opprettetKandidatlistehendelse)
        kandidatlisteRepository.lagreKandidatlistehendelse(oppdatertKandidatlistehendelse)

        val antallKandidatlister = kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()

        assertThat(antallKandidatlister).isEqualTo(1)
    }

    @Test
    fun `Tell antall kandidatlister tilknyttet direktemeldte stillinger`() {
    }

    @Test
    fun`Tell antall kandidatlister tilknyttet eksterne stillinger`() {
    }

    fun lagOpprettetKandidatlisteHendelse(stillingOpprettetTidspunkt: ZonedDateTime? = nowOslo(), kandidatlisteId: UUID = UUID.randomUUID()): Kandidatlistehendelse {
        return Kandidatlistehendelse(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            stillingensPubliseringstidspunkt = nowOslo(),
            organisasjonsnummer = "123123123",
            antallStillinger = 40,
            antallKandidater = 20,
            erDirektemeldt = true,
            kandidatlisteId = kandidatlisteId.toString(),
            tidspunkt = nowOslo(),
            stillingsId = UUID.randomUUID().toString(),
            utførtAvNavIdent = "A100100",
            eventName = opprettetKandidatlisteEventName
        )
    }

    fun lagOppdatertKandidatlisteHendelse(kandidatlisteId: UUID = UUID.randomUUID()): Kandidatlistehendelse {
        return Kandidatlistehendelse(
            stillingOpprettetTidspunkt = nowOslo(),
            stillingensPubliseringstidspunkt = nowOslo(),
            organisasjonsnummer = "123123123",
            antallStillinger = 40,
            antallKandidater = 20,
            erDirektemeldt = true,
            kandidatlisteId = kandidatlisteId.toString(),
            tidspunkt = nowOslo(),
            stillingsId = UUID.randomUUID().toString(),
            utførtAvNavIdent = "A100100",
            eventName = oppdaterteKandidatlisteEventName
        )
    }
}