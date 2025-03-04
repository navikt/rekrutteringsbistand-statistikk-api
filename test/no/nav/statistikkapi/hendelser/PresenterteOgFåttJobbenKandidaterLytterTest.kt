package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class PresenterteOgFåttJobbenKandidaterLytterTest {

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
        rapid.sendTestMessage(registrertDeltCvmelding())
        rapid.sendTestMessage(registrertDeltCvmelding())

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(1)
    }

    @Test
    fun `En melding skal ikke lagres dersom utfall er lik som på siste melding for samme kandidat og kandidatliste`() {
        val enMelding = registrertDeltCvmelding(nowOslo().minusHours(2))
        val enLikMeldingMenMedSenereTidspunkt = registrertDeltCvmelding(nowOslo())

        rapid.sendTestMessage(enMelding)
        assertThat(testRepository.hentUtfall()).hasSize(1)

        rapid.sendTestMessage(enLikMeldingMenMedSenereTidspunkt)
        assertThat(testRepository.hentUtfall()).hasSize(1)
    }

    @Test
    fun `mottak av kandidatutfall skalregisterers når det utfall endres`() {
        rapid.sendTestMessage(registrertDeltCvmelding())
        rapid.sendTestMessage(registrertFåttJobbenMelding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `Kan opprette kandidatutfall av RegistrertDeltCv-melding`() {
        rapid.sendTestMessage(registrertDeltCvmelding())

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(1)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T09:57:34.643+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat).isNotNull().isTrue()

            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isEqualTo(53)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("SPESIELT_TILPASSET_INNSATS")
            assertThat(hovedmål).isEqualTo("SKAFFEA")
        }
        stillingFraDb[0].apply {
            assertThat(uuid).isEqualTo("b2d427a4-061c-4ba4-890b-b7b0e04fb000")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }

    @Test
    fun `Kan opprette kandidatutfall av RegistrertDeltCv-melding usynlig kandidat uten inkludering`() {
        rapid.sendTestMessage(registrertDeltCvmeldingUsynligKandidatUtenInkludering)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(1)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T09:57:34.643+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat).isNotNull().isFalse()

            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isNull()
            assertThat(hullICv).isNull()
            assertThat(innsatsbehov).isNull()
            assertThat(hovedmål).isNull()
        }
        stillingFraDb[0].apply {
            assertThat(uuid).isEqualTo("b2d427a4-061c-4ba4-890b-b7b0e04fb000")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }

    @Test
    fun `Kan ikke opprette kandidatutfall av RegistrertDeltCv-melding med nullverdier for stilling`() {
        rapid.sendTestMessage(registrertDeltCvmeldingMedNullverdier)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).isEmpty()
        assertThat(stillingFraDb).isEmpty()
    }

    @Test
    fun `Vil ikke opprette kandidatutfall når RegistrertDeltCv-melding mangler stilling`() {
        rapid.sendTestMessage(registrerDeltCVMeldingUtenStillingberikelse)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).isEmpty()
    }

    @Test
    fun `Kan opprette kandidatutfall av RegistrertFåttJobben-melding`() {
        rapid.sendTestMessage(registrertFåttJobbenMelding)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(1)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T12:39:52.205+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.FATT_JOBBEN)
            assertThat(synligKandidat).isNotNull().isTrue()

            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isEqualTo(53)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("SPESIELT_TILPASSET_INNSATS")
            assertThat(hovedmål).isEqualTo("SKAFFEA")
        }

        stillingFraDb[0].apply {
            assertThat(uuid).isEqualTo("b2d427a4-061c-4ba4-890b-b7b0e04fb000")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }

    @Test
    fun `Vil ikke opprette kandidatutfall når RegistrertFåttJobben-melding mangler stilling`() {
        rapid.sendTestMessage(registrertFåttJobbenMeldingUtenStillingberikelse)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).isEmpty()
    }

    @Test
    fun `Skal kunne håndtere at melding inneholder tilretteleggingsbehov som er deprecated`() {
        rapid.sendTestMessage(registrertDeltCvMeldingMedDeprecatedTilretteleggingsbehov())

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(1)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b2d427a4-061c-4ba4-890b-b7b0e04fb000"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("6e22ced0-241b-4889-8285-7ca268d91b8d"))
            assertThat(navIdent).isEqualTo("Z990281")
            assertThat(navKontor).isEqualTo("0314")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-13T09:57:34.643+01:00").toLocalDateTime())
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat).isNotNull().isTrue()

            assertThat(aktorId).isEqualTo("2133747575903")
            assertThat(alder).isEqualTo(53)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("SPESIELT_TILPASSET_INNSATS")
            assertThat(hovedmål).isEqualTo("SKAFFEA")
        }
        stillingFraDb[0].apply {
            assertThat(uuid).isEqualTo("b2d427a4-061c-4ba4-890b-b7b0e04fb000")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }
}

private fun registrertDeltCvmelding(tidspunkt: ZonedDateTime = ZonedDateTime.parse( "2023-02-13T09:57:34.643+01:00").withZoneSameInstant(ZoneId.of("Europe/Oslo"))) = """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "$tidspunkt",
          "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": true,
          "inkludering": {
            "harHullICv": true,
            "alder": 53,
            "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
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
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
            "antallStillinger": 3,
            "organisasjonsnummer": "923282556",
            "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
          },
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()

private fun registrertDeltCvMeldingMedDeprecatedTilretteleggingsbehov(tidspunkt: ZonedDateTime = ZonedDateTime.parse( "2023-02-13T09:57:34.643+01:00").withZoneSameInstant(ZoneId.of("Europe/Oslo"))) = """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "$tidspunkt",
          "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": true,
          "inkludering": {
            "harHullICv": true,
            "alder": 53,
            "tilretteleggingsbehov": ["arbeidstid"],
            "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
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
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
            "antallStillinger": 3,
            "organisasjonsnummer": "923282556",
            "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
          },
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()

private val registrertDeltCvmeldingUsynligKandidatUtenInkludering = """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "2023-02-13T09:57:34.643+01:00",
          "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": false,
          "inkludering": null,
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
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
            "antallStillinger": 3,
            "organisasjonsnummer": "923282556",
            "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
          },
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()

private val registrertDeltCvmeldingMedNullverdier = """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "2023-02-13T09:57:34.643+01:00",
          "stillingsId": null,
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": true,
          "inkludering": null,
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
          "@forårsaket_av": {
            "id": "15379170-9d91-4670-bda1-94b4f4355131",
            "opprettet": "2023-02-13T09:58:01.055581269",
            "event_name": "kandidat_v2.RegistrertDeltCv"
          }
        }
    """.trimIndent()

private val registrerDeltCVMeldingUtenStillingberikelse = """
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
        "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
        "hovedmål": "SKAFFEA"
      },
      "@event_name": "kandidat_v2.RegistrertDeltCv"
    }
""".trimIndent()


private val registrertFåttJobbenMelding = """
    {
      "aktørId": "2133747575903",
      "organisasjonsnummer": "894822082",
      "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
      "tidspunkt": "2023-02-13T12:39:52.205+01:00",
      "stillingsId": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
      "utførtAvNavIdent": "Z990281",
      "utførtAvNavKontorKode": "0314",
      "synligKandidat": true,
      "inkludering": {
        "harHullICv": true,
        "alder": 53,
        "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
        "hovedmål": "SKAFFEA"
      },
      "@event_name": "kandidat_v2.RegistrertFåttJobben",
      "@id": "5f4531e7-f202-439b-88c0-68d14a05031f",
      "@opprettet": "2023-02-13T12:40:02.161234100",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "506fbed0-a263-432b-8f74-c4aa96587c90",
          "time": "2023-02-13T12:40:02.037858424",
          "service": "rekrutteringsbistand-stilling-api",
          "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
          "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
        },
        {
          "id": "5f4531e7-f202-439b-88c0-68d14a05031f",
          "time": "2023-02-13T12:40:02.161234100",
          "service": "rekrutteringsbistand-stilling-api",
          "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-dxkcj",
          "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:e9475052acb94e469ab72f0b2896830f12e3d23e"
        }
      ],
       "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "ergerg",
            "erDirektemeldt": true,
            "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
            "antallStillinger": 3,
            "organisasjonsnummer": "923282556",
            "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
          },
      "@forårsaket_av": {
        "id": "506fbed0-a263-432b-8f74-c4aa96587c90",
        "opprettet": "2023-02-13T12:40:02.037858424",
        "event_name": "kandidat_v2.RegistrertFåttJobben"
      }
    }
""".trimIndent()

val registrertFåttJobbenMeldingUtenStillingberikelse = """
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
        "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
        "hovedmål": "SKAFFEA"
      },
      "@event_name": "kandidat_v2.RegistrertFåttJobben"
    }
""".trimIndent()