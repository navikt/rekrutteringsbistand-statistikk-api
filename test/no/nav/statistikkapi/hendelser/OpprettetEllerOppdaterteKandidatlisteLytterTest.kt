package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatliste.Kandidatlistehendelse
import no.nav.statistikkapi.kandidatliste.oppdaterteKandidatlisteEventName
import no.nav.statistikkapi.kandidatliste.opprettetKandidatlisteEventName
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


class OpprettetEllerOppdaterteKandidatlisteLytterTest {
    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val kandidatlisteRepository = KandidatlisteRepository(database.dataSource)
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
        assertThat(kandidatlisterFraDB).hasSize(1)
        kandidatlisterFraDB[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("ebca044c-817a-4f9e-94ba-c73341c7d182"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("153b304c-9cf2-454c-a224-141914975487"))
            assertThat(erDirektemeldt).isEqualTo(true)
            assertThat(stillingOpprettetTidspunkt).isEqualTo(ZonedDateTime.parse("2023-01-06T08:57:40.751+01:00[Europe/Oslo]"))
            assertThat(antallStillinger).isEqualTo(1)
            assertThat(antallKandidater).isEqualTo(0)
            assertThat(tidspunkt).isEqualTo(tidspunktForHendelse)
        }
    }

    @Test
    fun `Skal motta oppdatert-melding og oppdatere eksisterende kandidatliste i databasen`() {
        val kandidatlisteId = UUID.randomUUID()
        kandidatlisteRepository.lagreKandidatlistehendelse(
            Kandidatlistehendelse(
                stillingOpprettetTidspunkt = nowOslo(),
                stillingensPubliseringstidspunkt = nowOslo(),
                organisasjonsnummer = "123123123",
                antallStillinger = 40,
                antallKandidater = 20,
                erDirektemeldt = true,
                kandidatlisteId = "$kandidatlisteId",
                tidspunkt = nowOslo(),
                stillingsId = UUID.randomUUID().toString(),
                utførtAvNavIdent = "A100100",
                eventName = oppdaterteKandidatlisteEventName
            )
        )

        assertThat(testRepository.hentKandidatlister()).hasSize(1)

        val tidspunkt = ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 1, 0), ZoneId.of("Europe/Oslo"))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(2)
        val oppdatertKandidatliste = kandidatlisterFraDB[1]
        assertThat(oppdatertKandidatliste.kandidatlisteId).isNotNull()
        assertThat(oppdatertKandidatliste.antallKandidater).isEqualTo(40)
        assertThat(oppdatertKandidatliste.antallStillinger).isEqualTo(20)
        assertThat(oppdatertKandidatliste.erDirektemeldt).isTrue()
        assertThat(oppdatertKandidatliste.stillingOpprettetTidspunkt).isNotNull()
        assertThat(oppdatertKandidatliste.dbId).isNotNull()
        assertThat(oppdatertKandidatliste.organisasjonsnummer).isEqualTo("312113341")
        assertThat(oppdatertKandidatliste.stillingensPubliseringstidspunkt).isNotNull()
        assertThat(oppdatertKandidatliste.stillingsId).isNotNull()
        assertThat(oppdatertKandidatliste.tidspunkt).isNotNull()
    }

    @Test
    fun `Vi skal ignorere oppdatert hendelser dersom det ikke finnes opprettet hendelse for samme kandidatliste`() {
        val kandidatlisteId = UUID.randomUUID()
        val tidspunkt = ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 1, 0), ZoneId.of("Europe/Oslo"))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).isEmpty()

    }

    @Test
    fun `Mottak av flere opprettet kandidatliste hendelser skal være idempotent i databasen`() {
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunkt))
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)
    }

    @Test
    fun `Mottak av flere endret kandidatliste hendelser skal være idempotent i databasen`() {
        val kandidatlisteId = UUID.randomUUID()
        kandidatlisteRepository.lagreKandidatlistehendelse(
            Kandidatlistehendelse(
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
                eventName = opprettetKandidatlisteEventName
            )
        )

        assertThat(testRepository.hentKandidatlister()).hasSize(1)

        val tidspunkt = ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 1, 0), ZoneId.of("Europe/Oslo"))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(2)
    }

    private fun oppdaterteKandidatlisteMelding(kandidatlisteId: UUID, tidspunktForHendelse: ZonedDateTime) = """
        {
          "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "antallStillinger": 20,
          "antallKandidater": 40,
          "erDirektemeldt": true,
          "stillingensPubliseringstidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "$kandidatlisteId",
          "tidspunkt": "${tidspunktForHendelse}",
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

    private fun opprettetKandidatlisteMelding(tidspunktForHendelsen: ZonedDateTime) = """
        {
          "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "antallStillinger": 1,
          "antallKandidater": 0,
          "erDirektemeldt": true,
          "stillingensPubliseringstidspunkt": "2023-01-06T08:57:40.75174+01:00",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "153b304c-9cf2-454c-a224-141914975487",
          "tidspunkt": "$tidspunktForHendelsen",
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
