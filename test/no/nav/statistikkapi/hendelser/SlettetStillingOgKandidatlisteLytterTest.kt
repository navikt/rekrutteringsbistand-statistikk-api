package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
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
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
                tilretteleggingsbehov = listOf("permittert", "arbeidstid")
            )
        )
        repository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                aktørId = "2258757075177",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
                tilretteleggingsbehov = listOf("permittert", "arbeidstid")
            )
        )

        testRepository.hentUtfall()

        rapid.sendTestMessage(melding)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).hasSize(4)
        utfallFraDb.find { u -> u.aktorId == "2258757075176" && u.utfall == Utfall.IKKE_PRESENTERT }!!.apply {
            assertThat(kandidatlisteId).isEqualTo("d5b5b4c1-0375-4719-9038-ab31fe27fb40")
            assertThat(stillingsId).isEqualTo("b5919e46-9882-4b3c-8089-53ad02f26023")
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isNull()
            assertThat(hovedmål).isNull()
            assertThat(alder).isNull()
            assertThat(tilretteleggingsbehov).isEmpty()
        }
        utfallFraDb.find { u -> u.aktorId == "2258757075177" && u.utfall == Utfall.IKKE_PRESENTERT }!!.apply {
            assertThat(kandidatlisteId).isEqualTo("d5b5b4c1-0375-4719-9038-ab31fe27fb40")
            assertThat(stillingsId).isEqualTo("b5919e46-9882-4b3c-8089-53ad02f26023")
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isNull()
            assertThat(hovedmål).isNull()
            assertThat(alder).isNull()
            assertThat(tilretteleggingsbehov).isEmpty()
        }
    }

    private val melding = """
        {
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
          "tidspunkt": "2023-02-09T09:45:53.649+01:00",
          "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
          "utførtAvNavIdent": "Z994633",
          "@event_name": "kandidat_v2.SlettetStillingOgKandidatliste",
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
            "event_name": "kandidat_v2.SlettetStillingOgKandidatliste"
          }
        }
    """.trimIndent()

}
