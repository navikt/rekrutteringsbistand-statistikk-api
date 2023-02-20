package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.*
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.kandidatutfall.Utfall.FATT_JOBBEN
import no.nav.statistikkapi.kandidatutfall.Utfall.PRESENTERT
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class SlettetStillingOgKandidatlisteLytterTest {

    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val testRepository = TestRepository(database.dataSource)
        private val repository = KandidatutfallRepository(database.dataSource)

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid, port = randomPort())
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleUtfall()
        testRepository.slettAlleStillinger()
        rapid.reset()
    }

    @Test
    fun `Kan annullere kandidatutfall med SlettetStillingOgKandidatliste-melding`() {
        val utfallPresentert = etKandidatutfall.copy(utfall = PRESENTERT, aktørId = aktørId1)
        val utfallFåttJobben = utfallPresentert.copy(utfall = Utfall.FATT_JOBBEN, aktørId = aktørId2)
        repository.lagreUtfall(utfallPresentert)
        repository.lagreUtfall(utfallFåttJobben)

        rapid.sendTestMessage(slettetStillingOgKandidatlisteMelding(utfallPresentert.kandidatlisteId))

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).hasSize(4)

        utfallFraDb.find { u -> u.aktorId == utfallPresentert.aktørId && u.utfall == Utfall.IKKE_PRESENTERT }!!.apply {
            assertThat(kandidatlisteId.toString()).isEqualTo(utfallPresentert.kandidatlisteId)
            assertThat(stillingsId.toString()).isEqualTo(utfallPresentert.stillingsId)
            assertThat(hullICv).isNull()
            assertThat(innsatsbehov).isNull()
            assertThat(hovedmål).isNull()
            assertThat(alder).isNull()
            assertThat(tilretteleggingsbehov).isEmpty()
        }
        utfallFraDb.find { u -> u.aktorId == utfallFåttJobben.aktørId && u.utfall == Utfall.IKKE_PRESENTERT }!!.apply {
            assertThat(kandidatlisteId.toString()).isEqualTo(utfallFåttJobben.kandidatlisteId)
            assertThat(stillingsId.toString()).isEqualTo(utfallFåttJobben.stillingsId)
            assertThat(hullICv).isNull()
            assertThat(innsatsbehov).isNull()
            assertThat(hovedmål).isNull()
            assertThat(alder).isNull()
            assertThat(tilretteleggingsbehov).isEmpty()
        }
    }

    @Test
    fun `Skal ikke lagre nytt utfall hvis siste utfall for en kandidat som er IKKE_PRESENTERT`() {
        val utfallPresentert = etKandidatutfall.copy(utfall = PRESENTERT, aktørId = aktørId1)
        val utfallIkkePresentert = utfallPresentert.copy(utfall = Utfall.IKKE_PRESENTERT, aktørId = aktørId2)
        repository.lagreUtfall(utfallPresentert)
        repository.lagreUtfall(utfallIkkePresentert)
        assertThat(testRepository.hentUtfall()).hasSize(2)

        rapid.sendTestMessage(slettetStillingOgKandidatlisteMelding(utfallIkkePresentert.kandidatlisteId))

        val utfallFraDatabase = testRepository.hentUtfall()
        assertThat(utfallFraDatabase).hasSize(3)
        val nyttUtfall = utfallFraDatabase.sortedByDescending { it.tidspunkt }.first()
        assertThat(nyttUtfall.aktorId).isEqualTo(utfallPresentert.aktørId)
        assertThat(nyttUtfall.utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
    }

    @Test
    fun `Skal ikke lagre nye utfall for andre kandidatlister`() {
        val utfallPresentert = etKandidatutfall.copy(utfall = PRESENTERT, aktørId = aktørId1)
        val utfallPresentertAnnenKandidatliste = etKandidatutfall.copy(kandidatlisteId = UUID.randomUUID().toString(), utfall = PRESENTERT, aktørId = aktørId1)
        repository.lagreUtfall(utfallPresentert)
        repository.lagreUtfall(utfallPresentertAnnenKandidatliste)
        assertThat(testRepository.hentUtfall()).hasSize(2)

        rapid.sendTestMessage(slettetStillingOgKandidatlisteMelding(utfallPresentert.kandidatlisteId))

        val utfallFraDatabase = testRepository.hentUtfall()
        assertThat(utfallFraDatabase).hasSize(3)
        val nyttUtfall = utfallFraDatabase.sortedByDescending { it.tidspunkt }.first()
        assertThat(nyttUtfall.kandidatlisteId.toString()).isEqualTo(utfallPresentert.kandidatlisteId)
        assertThat(nyttUtfall.aktorId).isEqualTo(utfallPresentert.aktørId)
        assertThat(nyttUtfall.utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
    }

    @Test
    fun `Skal bare lagre ett nytt kandidatutfall for kandidat som har blitt presentert, så fått jobben`() {
        val utfallPresentert = etKandidatutfall.copy(utfall = PRESENTERT, aktørId = aktørId1)
        val utfallFåttJobben = utfallPresentert.copy(utfall = FATT_JOBBEN)
        repository.lagreUtfall(utfallPresentert)
        repository.lagreUtfall(utfallFåttJobben)

        rapid.sendTestMessage(slettetStillingOgKandidatlisteMelding(utfallPresentert.kandidatlisteId))

        val utfallFraDatabase = testRepository.hentUtfall()
        assertThat(utfallFraDatabase).hasSize(3)
    }

    private fun slettetStillingOgKandidatlisteMelding(kandidatlisteId: String) = """
        {
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "$kandidatlisteId",
          "tidspunkt": "${nowOslo()}",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z994633",
          "@event_name": "kandidat_v2.SlettetStillingOgKandidatliste",
           "@id": "74b0b8dd-315f-406f-9979-e0bec5bcc5b6",
          "@opprettet": "2023-02-09T09:46:01.027221527"
        }
    """.trimIndent()
}
