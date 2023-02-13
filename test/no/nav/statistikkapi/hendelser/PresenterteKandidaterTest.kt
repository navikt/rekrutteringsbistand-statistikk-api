package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class RegistrertDeltCvTest {

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
    fun `mottak av kandidatutfall skal være idempotent`() {
        rapid.sendTestMessage(registrertDeltCvmelding)
        rapid.sendTestMessage(registrertDeltCvmelding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `Kan opprette kandidatutfall av RegistrertDeltCv melding`() {
        rapid.sendTestMessage(registrertDeltCvmelding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
        utfall[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(navIdent).isEqualTo("Z994633")
            assertThat(navKontor).isEqualTo("0313")
            assertThat(tidspunkt).isEqualTo(LocalDateTime.of(2023, 2, 9, 9, 45, 53, 649_000_000))
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat!!).isTrue()

            assertThat(aktorId).isEqualTo("2452127907551")
            assertThat(alder).isEqualTo(51)
            assertThat(tilretteleggingsbehov).isEmpty()
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isEqualTo("BFORM")
            assertThat(hovedmål).isEqualTo("BEHOLDEA")


        }
    }
}

private val registrertDeltCvmelding = """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "2023-02-13T09:57:34.643+01:00",
          "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
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

private val RegistrerDeltCVMeldingUtenStillingberikelse = """
    {
      "aktørId": "2133747575903",
      "organisasjonsnummer": "894822082",
      "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
      "tidspunkt": "2023-02-13T09:57:34.643+01:00",
      "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
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
      "@event_name": "kandidat_v2.RegistrertDeltCv"
    }
""".trimIndent()


private val RegistrertFåttJobbenMelding = """
    {
      "@event_name": "kandidat.registrer-fått-jobben",
      "kandidathendelse": {
        "type": "REGISTRER_FÅTT_JOBBEN",
        "aktørId": "2133747575903",
        "organisasjonsnummer": "894822082",
        "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
        "tidspunkt": "2023-02-13T10:03:02.145+01:00",
        "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
        "utførtAvNavIdent": "Z990281",
        "utførtAvNavKontorKode": "0314",
        "synligKandidat": true,
        "harHullICv": true,
        "alder": 53,
        "tilretteleggingsbehov": []
      },
      "@id": "afead715-1cfc-46dc-b69b-80ea00898a6b",
      "@opprettet": "2023-02-13T10:04:02.013956448",
      "system_read_count": 1,
      "system_participating_services": [
        {
          "id": "a17fda4e-61bc-47c6-9f4a-57bff6005f5b",
          "time": "2023-02-13T10:04:00.453680266",
          "service": "rekrutteringsbistand-stilling-api",
          "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
          "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
        },
        {
          "id": "36d83c05-38e7-4ea0-ac91-e3f381b655a9",
          "time": "2023-02-13T10:04:00.704140539",
          "service": "rekrutteringsbistand-stilling-api",
          "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
          "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
        },
        {
          "id": "36d83c05-38e7-4ea0-ac91-e3f381b655a9",
          "time": "2023-02-13T10:04:00.719440526",
          "service": "rekrutteringsbistand-statistikk-api",
          "instance": "rekrutteringsbistand-statistikk-api-7c56bd44fb-2h8zx",
          "image": "ghcr.io/navikt/rekrutteringsbistand-statistikk-api/rekrutteringsbistand-statistikk-api:8f68a4297dc296971a136eab65a7c729df82991e"
        },
        {
          "id": "afead715-1cfc-46dc-b69b-80ea00898a6b",
          "time": "2023-02-13T10:04:02.013956448",
          "service": "rekrutteringsbistand-statistikk-api",
          "instance": "rekrutteringsbistand-statistikk-api-7c56bd44fb-2h8zx",
          "image": "ghcr.io/navikt/rekrutteringsbistand-statistikk-api/rekrutteringsbistand-statistikk-api:8f68a4297dc296971a136eab65a7c729df82991e"
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
        "id": "36d83c05-38e7-4ea0-ac91-e3f381b655a9",
        "opprettet": "2023-02-13T10:04:00.704140539",
        "event_name": "kandidat.registrer-fått-jobben"
      },
      "@slutt_av_hendelseskjede": true
    }
""".trimIndent()

val RegistrertFåttJobbenMeldingUtenStilling = """
    {
      "aktørId": "2133747575903",
      "organisasjonsnummer": "894822082",
      "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
      "tidspunkt": "2023-02-13T10:03:02.145+01:00",
      "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
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
      "@event_name": "kandidat_v2.RegistrertFåttJobben"
    }
""".trimIndent()