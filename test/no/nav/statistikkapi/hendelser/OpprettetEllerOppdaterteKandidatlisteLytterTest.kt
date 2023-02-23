package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*


class OpprettetEllerOppdaterteKandidatlisteLytterTest {
    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val testRepository = TestRepository(database.dataSource)

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid, port = randomPort())
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleKandidatlister()
        rapid.reset()
    }

    @Test
    fun `Skal motta melding om opprettet kandidatliste og lagre i databasen`() {
        val tidspunktForHendelse = nowOslo()
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunktForHendelse))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        kandidatlisterFraDB[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("fbf8c658-469a-44b8-8c0e-5f2013f1b835"))
            assertThat(navIdent).isEqualTo("Z994241")
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("fc63163c-96f9-491f-8c4e-e5bd0d35b463"))
            assertThat(erDirektemeldt).isEqualTo(true)
            assertThat(stillingOpprettetTidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-03T13:56:11.354599+01:00").toLocalDateTime())
            assertThat(antallStillinger).isEqualTo(1)
            assertThat(antallKandidater).isEqualTo(0)
            assertThat(tidspunkt).isEqualTo(tidspunktForHendelse.toLocalDateTime())
        }
    }

    @Test
    fun `Skal motta oppdatert-melding og oppdatere eksisterende kandidatliste i databasen`() {
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(tidspunkt.minusHours(2)))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)
    }

    @Test
    fun `Mottak av opprettet kandidatliste skal være idempotent`() {
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunkt))
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)
    }

    private fun oppdaterteKandidatlisteMelding(tidspunktForHendelse: ZonedDateTime) = """
        {
          "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "antallStillinger": 1,
          "antallKandidater": 4,
          "erDirektemeldt": true,
          "stillingensPubliseringstidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "153b304c-9cf2-454c-a224-141914975487",
          "tidspunkt": ${tidspunktForHendelse},
          "stillingsId": "ebca044c-817a-4f9e-94ba-c73341c7d182",
          "utførtAvNavIdent": "Z994086",
          "@event_name": "kandidat_v2.OppdaterteKandidatliste",
          "system_participating_services": [
            {
              "id": "131d6070-8ad1-44ec-ab81-92595e3e0ab2",
              "time": "2023-02-23T13:28:30.009883591",
              "service": "rekrutteringsbistand-kandidat-api",
              "instance": "rekrutteringsbistand-kandidat-api-dfc77c7b4-qrr69",
              "image": "ghcr.io/navikt/rekrutteringsbistand-kandidat-api/rekrutteringsbistand-kandidat-api:6a09c2c38b8f925b4a8290c5e07c348d02c30824"
            }
          ]
        }
    """.trimIndent()

    private fun opprettetKandidatlisteMelding(tidspunktForHendelse: ZonedDateTime) = """
        {
          "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "antallStillinger": 1,
          "antallKandidater": 0,
          "erDirektemeldt": true,
          "stillingensPubliseringstidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "153b304c-9cf2-454c-a224-141914975487",
          "tidspunkt": ${tidspunktForHendelse},
          "stillingsId": "ebca044c-817a-4f9e-94ba-c73341c7d182",
          "utførtAvNavIdent": "Z994086",
          "@event_name": "kandidat_v2.OpprettetKandidatliste",
          "system_participating_services": [
            {
              "id": "131d6070-8ad1-44ec-ab81-92595e3e0ab2",
              "time": "2023-02-23T13:28:30.009883591",
              "service": "rekrutteringsbistand-kandidat-api",
              "instance": "rekrutteringsbistand-kandidat-api-dfc77c7b4-qrr69",
              "image": "ghcr.io/navikt/rekrutteringsbistand-kandidat-api/rekrutteringsbistand-kandidat-api:6a09c2c38b8f925b4a8290c5e07c348d02c30824"
            }
          ]
        }
    """.trimIndent()
}