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
import java.time.LocalDateTime
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
        rapid.sendTestMessage(melding)
        rapid.sendTestMessage(melding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `Kan opprette kandidatutfall av DelCvMedArbeidsgiver-melding`() {
        rapid.sendTestMessage(melding)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(2)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb.find { u -> u.aktorId == "2452127907551" }!!.apply {
            assertThat(alder).isEqualTo(51)
            assertThat(tilretteleggingsbehov).isEmpty()
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isEqualTo("BFORM")
            assertThat(hovedmål).isEqualTo("BEHOLDEA")
        }
        utfallFraDb.find { it.aktorId == "2452127907123" }!!.apply {
            assertThat(alder).isEqualTo(24)
            assertThat(tilretteleggingsbehov).containsExactly("arbeidstid")
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

    private val melding = """
        {
          "stillingstittel": "En fantastisk stilling",
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "2023-02-09T09:45:53.649+01:00",
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

}
