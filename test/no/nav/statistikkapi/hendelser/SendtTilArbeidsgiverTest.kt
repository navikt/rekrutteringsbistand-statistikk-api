package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.*
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class SendtTilArbeidsgiverTest {

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
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(melding(tidspunkt))
        rapid.sendTestMessage(melding(tidspunkt))

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `En melding skal ikke lagres dersom utfall er lik som på siste melding for samme kandidat og kandidatliste`() {
        val enMelding = melding(nowOslo().minusHours(2))
        val enLikMeldingMenMedSenereTidspunkt = melding(nowOslo())

        rapid.sendTestMessage(enMelding)
        assertThat(testRepository.hentUtfall()).hasSize(2)

        rapid.sendTestMessage(enLikMeldingMenMedSenereTidspunkt)
        assertThat(testRepository.hentUtfall()).hasSize(2)
    }

    @Test
    fun `Kan opprette kandidatutfall av DelCvMedArbeidsgiver-melding`() {
        rapid.sendTestMessage(melding())

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(2)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb.find { u -> u.aktorId == "2452127907551" }!!.apply {
            assertThat(alder).isEqualTo(51)
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isEqualTo("BFORM")
            assertThat(hovedmål).isEqualTo("BEHOLDEA")
        }
        utfallFraDb.find { it.aktorId == "2452127907123" }!!.apply {
            assertThat(alder).isEqualTo(24)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("VARIG")
            assertThat(hovedmål).isEqualTo("SKAFFERA")

        }
        utfallFraDb.forEach {
            assertThat(it.stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(it.kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(it.navIdent).isEqualTo("Z994633")
            assertThat(it.navKontor).isEqualTo("0313")
            assertThat(it.tidspunkt).isEqualTo(LocalDateTime.of(2023, 2, 9, 9, 45, 53, 649_000_000))
            assertThat(it.utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(it.synligKandidat).isNotNull().isTrue()
        }

        stillingFraDb[0].apply {
            this!!
            assertThat(uuid).isEqualTo("b5919e46-9882-4b3c-8089-53ad02f26023")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }

    @Test
    fun `Kan opprette kandidatutfall av DelCvMedArbeidsgiver-melding også når den inneholder tilretteleggingsbehov som er deprecated`() {
        rapid.sendTestMessage(meldingMedTilretteleggingsbehovSomErDeprecated())

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(2)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb.find { u -> u.aktorId == "2452127907551" }!!.apply {
            assertThat(alder).isEqualTo(51)
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isEqualTo("BFORM")
            assertThat(hovedmål).isEqualTo("BEHOLDEA")
        }
        utfallFraDb.find { it.aktorId == "2452127907123" }!!.apply {
            assertThat(alder).isEqualTo(24)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("VARIG")
            assertThat(hovedmål).isEqualTo("SKAFFERA")

        }
        utfallFraDb.forEach {
            assertThat(it.stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(it.kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(it.navIdent).isEqualTo("Z994633")
            assertThat(it.navKontor).isEqualTo("0313")
            assertThat(it.tidspunkt).isEqualTo(LocalDateTime.of(2023, 2, 9, 9, 45, 53, 649_000_000))
            assertThat(it.utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(it.synligKandidat).isNotNull().isTrue()
        }

        stillingFraDb[0].apply {
            this!!
            assertThat(uuid).isEqualTo("b5919e46-9882-4b3c-8089-53ad02f26023")
            assertThat(stillingskategori).isEqualTo(Stillingskategori.STILLING)
        }
    }

    @Test
    fun `Kan opprette kandidatutfall av DelCvMedArbeidsgiver-melding med stilllingskategori null`() {
        rapid.sendTestMessage(meldingUtenStillingskategori)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(2)
        assertThat(stillingFraDb).hasSize(1)
        assertThat(stillingFraDb[0].stillingskategori).isEqualTo(Stillingskategori.STILLING)
    }

    private fun melding(tidspunkt: ZonedDateTime = ZonedDateTime.parse("2023-02-09T09:45:53.649+01:00").withZoneSameInstant(ZoneId.of("Europe/Oslo"))) = """
        {
          "stillingstittel": "En fantastisk stilling",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "$tidspunkt",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z994633",
          "utførtAvNavKontorKode": "0313",
          "utførtAvVeilederFornavn": "F_Z994633",
          "utførtAvVeilederEtternavn": "E_Z994633",
          "arbeidsgiversEpostadresser": [
            "hei@arbeidsgiversdomene.no",
            "enansatt@trygdeetaten.no"
          ],
          "meldingTilArbeidsgiver": "Hei, her er en god kandidat som vil føre til at du kan selge varene dine med høyere avanse!",
          "kandidater": {
            "2452127907551": {
              "harHullICv": false,
              "alder": 51,
              "tilretteleggingsbehov": [],
              "innsatsbehov": "BFORM",
              "hovedmål": "BEHOLDEA"
            },
            "2452127907123": {
              "harHullICv": true,
              "alder": 24,
              "innsatsbehov": "VARIG",
              "hovedmål": "SKAFFERA"
            }
          },
          "@event_name": "kandidat_v2.DelCvMedArbeidsgiver",
          "@id": "74b0b8dd-315f-406f-9979-e0bec5bcc5b6",
          "@opprettet": "2023-02-09T09:46:01.027221527",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "6f52c0ce-0b66-4ee6-981b-3952113d225a",
              "time": "2023-02-09T09:46:00.879931755",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-5r8dn",
              "image": "erere"
            },
            {
              "id": "74b0b8dd-315f-406f-9979-e0bec5bcc5b6",
              "time": "2023-02-09T09:46:01.027221527",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-5r8dn",
              "image": "fdgsgsg"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "d55c3510-d263-42da-8785-3c92d3eb8732",
            "stillingsid": "b5919e46-9882-4b3c-8089-53ad02f26023",
            "eier": null,
            "notat": null,
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "En fantastisk stilling"
          },
          "@forårsaket_av": {
            "id": "6f52c0ce-0b66-4ee6-981b-3952113d225a",
            "opprettet": "2023-02-09T09:46:00.879931755",
            "event_name": "kandidat_v2.DelCvMedArbeidsgiver"
          }
        }
    """.trimIndent()

    private fun meldingMedTilretteleggingsbehovSomErDeprecated(tidspunkt: ZonedDateTime = ZonedDateTime.parse("2023-02-09T09:45:53.649+01:00").withZoneSameInstant(ZoneId.of("Europe/Oslo"))) = """
        {
          "stillingstittel": "En fantastisk stilling",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "$tidspunkt",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z994633",
          "utførtAvNavKontorKode": "0313",
          "utførtAvVeilederFornavn": "F_Z994633",
          "utførtAvVeilederEtternavn": "E_Z994633",
          "arbeidsgiversEpostadresser": [
            "hei@arbeidsgiversdomene.no",
            "enansatt@trygdeetaten.no"
          ],
          "meldingTilArbeidsgiver": "Hei, her er en god kandidat som vil føre til at du kan selge varene dine med høyere avanse!",
          "kandidater": {
            "2452127907551": {
              "harHullICv": false,
              "alder": 51,
              "tilretteleggingsbehov": ["arbeidstid"],
              "innsatsbehov": "BFORM",
              "hovedmål": "BEHOLDEA"
            },
            "2452127907123": {
              "harHullICv": true,
              "alder": 24,
              "tilretteleggingsbehov": ["arbeidstid"],
              "innsatsbehov": "VARIG",
              "hovedmål": "SKAFFERA"
            }
          },
          "@event_name": "kandidat_v2.DelCvMedArbeidsgiver",
          "@id": "74b0b8dd-315f-406f-9979-e0bec5bcc5b6",
          "@opprettet": "2023-02-09T09:46:01.027221527",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "6f52c0ce-0b66-4ee6-981b-3952113d225a",
              "time": "2023-02-09T09:46:00.879931755",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-5r8dn",
              "image": "erere"
            },
            {
              "id": "74b0b8dd-315f-406f-9979-e0bec5bcc5b6",
              "time": "2023-02-09T09:46:01.027221527",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-675cfbd5fb-5r8dn",
              "image": "fdgsgsg"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "d55c3510-d263-42da-8785-3c92d3eb8732",
            "stillingsid": "b5919e46-9882-4b3c-8089-53ad02f26023",
            "eier": null,
            "notat": null,
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "En fantastisk stilling"
          },
          "@forårsaket_av": {
            "id": "6f52c0ce-0b66-4ee6-981b-3952113d225a",
            "opprettet": "2023-02-09T09:46:00.879931755",
            "event_name": "kandidat_v2.DelCvMedArbeidsgiver"
          }
        }
    """.trimIndent()

    val meldingUtenStillingskategori = """
        {
          "stillingstittel": "Selger - GEDDON MALE KVADRAT",
          "organisasjonsnummer": "922332819",
          "kandidatlisteId": "83efd2dc-20dc-471c-90b1-fd819a941365",
          "tidspunkt": "2023-02-13T16:02:49.893+01:00",
          "stillingsId": "f3d5f94a-1931-4b6c-a27a-3910921ab3ba",
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "utførtAvVeilederFornavn": "F_Z990281",
          "utførtAvVeilederEtternavn": "E_Z990281",
          "arbeidsgiversEpostadresser": [
            "joar.aurdal@nav.no",
            "F_Z990281.E_Z990281@trygdeetaten.no"
          ],
          "meldingTilArbeidsgiver": "hei, dette er en test",
          "kandidater": {
            "2920221987929": {
              "harHullICv": true,
              "alder": 53,
              "innsatsbehov": "BATT",
              "hovedmål": "SKAFFEA"
            },
            "2398700201334": {
              "harHullICv": false,
              "alder": 43,
              "tilretteleggingsbehov": [],
              "innsatsbehov": "BATT",
              "hovedmål": "SKAFFEA"
            }
          },
          "@event_name": "kandidat_v2.DelCvMedArbeidsgiver",
          "@id": "c56e15d0-5f3c-41f1-ad34-2181946b2e6b",
          "@opprettet": "2023-02-13T16:03:01.408500206",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "827197b4-70c6-4ad4-bff8-4d86439f0896",
              "time": "2023-02-13T16:03:01.280827429",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7b55b58b8-74nhl",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:62b51dc84bee899cada734cb9dc0ad9ccf702b78"
            },
            {
              "id": "c56e15d0-5f3c-41f1-ad34-2181946b2e6b",
              "time": "2023-02-13T16:03:01.408500206",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-7b55b58b8-74nhl",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:62b51dc84bee899cada734cb9dc0ad9ccf702b78"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "fcc512ae-f4db-4f4d-9488-903ad1ce5648",
            "stillingsid": "f3d5f94a-1931-4b6c-a27a-3910921ab3ba",
            "eier": {
              "navident": "Z990281",
              "navn": "F_Z990281 E_Z990281"
            },
            "notat": null,
            "stillingskategori": null
          },
          "stilling": {
            "stillingstittel": "Selger - GEDDON MALE KVADRAT"
          },
          "@forårsaket_av": {
            "id": "827197b4-70c6-4ad4-bff8-4d86439f0896",
            "opprettet": "2023-02-13T16:03:01.280827429",
            "event_name": "kandidat_v2.DelCvMedArbeidsgiver"
          }
        }
    """.trimIndent()

}
