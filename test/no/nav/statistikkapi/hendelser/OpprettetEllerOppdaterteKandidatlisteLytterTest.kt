package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatliste.*
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.LoggerFactory
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
            setUpLogWatcher()
        }

        private lateinit var logWatcher: ListAppender<ILoggingEvent>

        private fun setUpLogWatcher() {
            logWatcher = ListAppender<ILoggingEvent>()
            logWatcher.start()
            val logger =
                LoggerFactory.getLogger(KandidatlistehendelseLytter::class.java) as ch.qos.logback.classic.Logger
            logger.addAppender(logWatcher)
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleKandidatlister()
        rapid.reset()

        setUpLogWatcher()
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
    fun `Skal ignorere melding om opprettet kandidatliste som ikke har stilling`() {
        val tidspunktForHendelse = nowOslo()
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunktForHendelse, harStilling = false))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(0)

        assertIngenFeilILogg()
    }

    @Test
    fun `Skal ignorere melding om opprettet kandidatliste om stillinger som ikke er publiserte`() {
        val tidspunktForHendelse = nowOslo()
        rapid.sendTestMessage(opprettetKandidatlisteMelding(tidspunktForHendelse, stillingensPubliseringstidspunkt = null))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(0)

        assertIngenFeilILogg()
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
    fun `Skal ignorere oppdatert-melding og om stilling ikke finnes på hendelse`() {
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
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt, harStilling = false))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)

        assertIngenFeilILogg()
    }

    @Test
    fun `Skal ignorere oppdatert-melding om stillingen ikke er publisert`() {
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
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt, stillingensPubliseringstidspunkt = null))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)

        assertIngenFeilILogg()
    }

    private fun assertIngenFeilILogg() {
        val warningsOgErrorsLogglinjer = logWatcher.list.filter { it.level == Level.ERROR || it.level == Level.WARN }
        assertThat(warningsOgErrorsLogglinjer).isEmpty()
    }

    @Test
    fun `Vi skal lagre oppdatert hendelser også dersom det ikke finnes opprettet hendelse for samme kandidatliste`() {
        val kandidatlisteId = UUID.randomUUID()
        val tidspunkt = ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 1, 0), ZoneId.of("Europe/Oslo"))
        rapid.sendTestMessage(oppdaterteKandidatlisteMelding(kandidatlisteId, tidspunkt))

        val kandidatlisterFraDB = testRepository.hentKandidatlister()
        assertThat(kandidatlisterFraDB).hasSize(1)
        assertThat(kandidatlisterFraDB[0].eventName).isEqualTo(oppdaterteKandidatlisteEventName)
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

    private fun oppdaterteKandidatlisteMelding(
        kandidatlisteId: UUID,
        tidspunktForHendelse: ZonedDateTime,
        harStilling: Boolean = true,
        stillingensPubliseringstidspunkt: String? = "2023-01-06T08:57:40.751740+01:00[Europe/Oslo]"
    ) = """
        {
          "antallKandidater": 40,
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "$kandidatlisteId",
          "tidspunkt": "${tidspunktForHendelse}",
          "stillingsId": "ebca044c-817a-4f9e-94ba-c73341c7d182",
          "utførtAvNavIdent": "Z994086",
          "@event_name": "kandidat_v2.OppdaterteKandidatliste",
          "system_participating_services": [
            {
              "id": "c401cdfd-1dbd-4796-ae8a-574d60d61358",
              "time": "2023-03-02T11:55:00.005653899",
              "service": "rekrutteringsbistand-kandidat-api",
              "instance": "rekrutteringsbistand-kandidat-api-7d9d79fbd4-rtd4x",
              "image": "ghcr.io/navikt/rekrutteringsbistand-kandidat-api/rekrutteringsbistand-kandidat-api:a7c562a9c6589b78d17096256163cd803f7b8c31"
            },
            {
              "id": "3ab3fb46-92eb-4b84-aab4-6c81cc09d87c",
              "time": "2023-03-02T11:55:00.055783457",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7c56bff44b-7wrzs",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:a8bc3d869653c84047262c5760b4946f73e6b44f"
            },
            {
              "id": "a35579e8-cef5-42ad-b546-baf720b4eb3e",
              "time": "2023-03-02T11:55:00.150721581",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7c56bff44b-7wrzs",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:a8bc3d869653c84047262c5760b4946f73e6b44f"
            }
          ],
          "@id": "a35579e8-cef5-42ad-b546-baf720b4eb3e",
          "@opprettet": "2023-03-02T11:55:00.150721581",
          "system_read_count": 0,
          "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "ebca044c-817a-4f9e-94ba-c73341c7d182",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          ${if(harStilling)""""stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.751740+01:00[Europe/Oslo]",
            "antallStillinger": 20,
            "organisasjonsnummer": "312113341",
            "stillingensPubliseringstidspunkt": ${stillingensPubliseringstidspunkt?.let { "\"$it\"" }}
          },""" else ""}
          "@forårsaket_av": {
            "id": "3ab3fb46-92eb-4b84-aab4-6c81cc09d87c",
            "opprettet": "2023-03-02T11:55:00.055783457",
            "event_name": "kandidat_v2.OppdaterteKandidatliste"
          }
        }
    """.trimIndent()

    private fun opprettetKandidatlisteMelding(
        tidspunktForHendelsen: ZonedDateTime, harStilling: Boolean = true,
        stillingensPubliseringstidspunkt: String? = "2023-01-06T08:57:40.751740+01:00[Europe/Oslo]"
    ) = """
        {
          "antallKandidater": 0,
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "153b304c-9cf2-454c-a224-141914975487",
          "tidspunkt": "$tidspunktForHendelsen",
          "stillingsId": "ebca044c-817a-4f9e-94ba-c73341c7d182",
          "utførtAvNavIdent": "Z994086",
          "@event_name": "kandidat_v2.OpprettetKandidatliste",
          "system_participating_services": [
            {
              "id": "c401cdfd-1dbd-4796-ae8a-574d60d61358",
              "time": "2023-03-02T11:55:00.005653899",
              "service": "rekrutteringsbistand-kandidat-api",
              "instance": "rekrutteringsbistand-kandidat-api-7d9d79fbd4-rtd4x",
              "image": "ghcr.io/navikt/rekrutteringsbistand-kandidat-api/rekrutteringsbistand-kandidat-api:a7c562a9c6589b78d17096256163cd803f7b8c31"
            },
            {
              "id": "3ab3fb46-92eb-4b84-aab4-6c81cc09d87c",
              "time": "2023-03-02T11:55:00.055783457",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7c56bff44b-7wrzs",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:a8bc3d869653c84047262c5760b4946f73e6b44f"
            },
            {
              "id": "a35579e8-cef5-42ad-b546-baf720b4eb3e",
              "time": "2023-03-02T11:55:00.150721581",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7c56bff44b-7wrzs",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:a8bc3d869653c84047262c5760b4946f73e6b44f"
            }
          ],
          "@id": "a35579e8-cef5-42ad-b546-baf720b4eb3e",
          "@opprettet": "2023-03-02T11:55:00.150721581",
          "system_read_count": 0,
          "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "ebca044c-817a-4f9e-94ba-c73341c7d182",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          ${if(harStilling)""""stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2023-01-06T08:57:40.751740+01:00[Europe/Oslo]",
            "antallStillinger": 1,
            "organisasjonsnummer": "312113341",
            "stillingensPubliseringstidspunkt": ${stillingensPubliseringstidspunkt?.let { "\"$it\"" }}
          },""" else ""}
          "@forårsaket_av": {
            "id": "3ab3fb46-92eb-4b84-aab4-6c81cc09d87c",
            "opprettet": "2023-03-02T11:55:00.055783457",
            "event_name": "kandidat_v2.OppdaterteKandidatliste"
          }
        }
    """.trimIndent()
}
