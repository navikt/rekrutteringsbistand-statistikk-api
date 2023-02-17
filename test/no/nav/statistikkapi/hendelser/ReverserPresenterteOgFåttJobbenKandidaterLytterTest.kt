package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*

class ReverserPresenterteOgFåttJobbenKandidaterLytterTest {

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
        testRepository.slettAlleUtfall()
        testRepository.slettAlleStillinger()
        rapid.reset()
    }

    @Test
    fun `mottak av kandidatutfall skal registerers når utfall endres`() {
        rapid.sendTestMessage(registrertDeltCvmelding)
        assertThat(testRepository.hentUtfall()).size().isEqualTo(1)


        rapid.sendTestMessage(fjernetRegistreringDeltCvMelding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `mottak av kandidatutfall skal være idempotent`() {
        rapid.sendTestMessage(registrertDeltCvmelding)
        assertThat(testRepository.hentUtfall()).size().isEqualTo(1)


        rapid.sendTestMessage(fjernetRegistreringDeltCvMelding)
        rapid.sendTestMessage(fjernetRegistreringDeltCvMelding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }



    @Test
    fun `Kan opprette kandidatutfall av FjernetRegistreringDeltCv-melding`() {
        rapid.sendTestMessage(registrertDeltCvmelding)
        rapid.sendTestMessage(fjernetRegistreringDeltCvMelding)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).hasSize(2)
        utfallFraDb[1].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T09:57:34.643+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
            assertThat(synligKandidat).isNotNull().isTrue()

            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isEqualTo(53)
            assertThat(tilretteleggingsbehov).isEmpty()
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("BATT")
            assertThat(hovedmål).isEqualTo("SKAFFEA")
        }
    }

    @Test
    fun `Kan opprette kandidatutfall av FjernetRegistreringFåttJobben-melding`() {
        rapid.sendTestMessage(registrertFåttJobbenMelding)
        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).hasSize(2)
        utfallFraDb[1].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T09:57:34.643+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat).isNotNull().isTrue()
            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isEqualTo(53)
            assertThat(tilretteleggingsbehov).isEmpty()
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("BATT")
            assertThat(hovedmål).isEqualTo("SKAFFEA")
        }
    }
}

private val fjernetRegistreringDeltCvMelding = """
    {
      "aktørId": "2258757075176",
      "organisasjonsnummer": "312113341",
      "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
      "tidspunkt": "2023-02-16T10:38:25.877+01:00",
      "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
      "utførtAvNavIdent": "Z994633",
      "utførtAvNavKontorKode": "0313",
      "@event_name": "kandidat_v2.FjernetRegistreringDeltCv"
    }
""".trimIndent()


private val fjernetRegistreringFåttJobbenMelding = """
    {
      "aktørId": "2258757075176",
      "organisasjonsnummer": "312113341",
      "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
      "tidspunkt": "2023-02-16T10:38:25.877+01:00",
      "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
      "utførtAvNavIdent": "Z994633",
      "utførtAvNavKontorKode": "0313",
      "@event_name": "kandidat_v2.FjernetRegistreringFåttJobben"
    }
""".trimIndent()

private val registrertDeltCvmelding = """
        {
          "aktørId": "2258757075176",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "2023-02-13T09:57:34.643+01:00",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": true,
          "inkludering": {
            "harHullICv": true,
            "alder": 53,
            "tilretteleggingsbehov": [],
            "innsatsbehov": "BATT",
            "hovedmål": "SKAFFEA"
          },
          "@event_name": "kandidat_v2.RegistrertDeltCv",
          "@id": "1bbc0be5-8eb0-4d77-a64f-53bdad97de39",
          "@opprettet": "2023-02-13T09:58:03.191128099",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "15379170-9d91-4670-bda1-94b4f4355131",
              "time": "2023-02-13T09:58:01.055581269",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
            },
            {
              "id": "1bbc0be5-8eb0-4d77-a64f-53bdad97de39",
              "time": "2023-02-13T09:58:03.191128099",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "0f5daf9c-e6c6-4001-86bb-f90f812e40e7",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": null,
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "Ny stilling"
          },
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()

private val registrertFåttJobbenMelding = """
        {
          "aktørId": "2258757075176",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "2023-02-13T09:57:34.643+01:00",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": true,
          "inkludering": {
            "harHullICv": true,
            "alder": 53,
            "tilretteleggingsbehov": [],
            "innsatsbehov": "BATT",
            "hovedmål": "SKAFFEA"
          },
          "@event_name": "kandidat_v2.RegistrertFåttJobben",
          "@id": "1bbc0be5-8eb0-4d77-a64f-53bdad97de39",
          "@opprettet": "2023-02-13T09:58:03.191128099",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "15379170-9d91-4670-bda1-94b4f4355131",
              "time": "2023-02-13T09:58:01.055581269",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
            },
            {
              "id": "1bbc0be5-8eb0-4d77-a64f-53bdad97de39",
              "time": "2023-02-13T09:58:03.191128099",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "0f5daf9c-e6c6-4001-86bb-f90f812e40e7",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": null,
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "Ny stilling"
          },
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()
