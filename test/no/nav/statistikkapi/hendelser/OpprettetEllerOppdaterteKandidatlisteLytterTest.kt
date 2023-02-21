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
import java.time.ZoneId
import java.time.ZonedDateTime

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
        testRepository.slettAlleStillinger()
        rapid.reset()
    }

    @Test
    fun `skal motta melding om kandidatliste og lagre i databasen`() {
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(opprettetEllerOppdaterteKandidatlisteMelding(tidspunkt))
        val kandidatliste = testRepository.hentKandidatlister()[1]
        assertThat(kandidatliste).isEqualTo(opprettetEllerOppdaterteKandidatlisteMelding(tidspunkt))
    }

    @Test
    fun `mottak av kandidatliste skal være idempotent`() {
        val tidspunkt = nowOslo()
        rapid.sendTestMessage(opprettetEllerOppdaterteKandidatlisteMelding(tidspunkt))
        rapid.sendTestMessage(opprettetEllerOppdaterteKandidatlisteMelding(tidspunkt))
        val kandidatliste = testRepository.hentKandidatlister()
        assertThat(kandidatliste).hasSize(1)
    }

    private fun opprettetEllerOppdaterteKandidatlisteMelding(tidspunkt: ZonedDateTime = ZonedDateTime.parse("2023-02-20T12:41:13.303+01:00").withZoneSameInstant(ZoneId.of("Europe/Oslo"))) = """
        {
          "stillingOpprettetTidspunkt": "2023-02-03T13:56:11.354599+01:00",
          "antallStillinger": 1,
          "erDirektemeldt": true,
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "fc63163c-96f9-491f-8c4e-e5bd0d35b463",
          "tidspunkt": "$tidspunkt",
          "stillingsId": "fbf8c658-469a-44b8-8c0e-5f2013f1b835",
          "utførtAvNavIdent": "Z994241",
          "@event_name": "kandidat_v2.OpprettetEllerOppdaterteKandidatliste",
          "@id": "a18ba861-b6ea-4591-8bff-500dae5cc988",
          "@opprettet": "2023-02-20T12:42:00.368799706",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "492f0677-95c4-4b6b-aa05-802b16f05a8f",
              "time": "2023-02-20T12:42:00.046146776",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-77b44d8f66-kcvsb",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:b8a0313caaca50c1e639dd8fa461db202eecd8ef"
            },
            {
              "id": "a18ba861-b6ea-4591-8bff-500dae5cc988",
              "time": "2023-02-20T12:42:00.368799706",
              "service": "rekrutteringsbistand-stilling-api",
              "instance": "rekrutteringsbistand-stilling-api-77b44d8f66-kcvsb",
              "image": "ghcr.io/navikt/rekrutteringsbistand-stilling-api/rekrutteringsbistand-stilling-api:b8a0313caaca50c1e639dd8fa461db202eecd8ef"
            }
          ],
          "stillingsinfo": {
            "stillingsinfoid": "a89bfdf9-c02f-4093-b360-340bff6ccc1b",
            "stillingsid": "fbf8c658-469a-44b8-8c0e-5f2013f1b835",
            "eier": null,
            "notat": null,
            "stillingskategori": "STILLING"
          },
          "stilling": {
            "stillingstittel": "Superengasjert, morsom lærer til Oslo-skole",
            "erDirektemeldt": true
          },
          "@forårsaket_av": {
            "id": "492f0677-95c4-4b6b-aa05-802b16f05a8f",
            "opprettet": "2023-02-20T12:42:00.046146776",
            "event_name": "kandidat_v2.OpprettetEllerOppdaterteKandidatliste"
          }
        }
    """.trimIndent()

    val opprettetEllerOppdaterteKandidatlisteMeldingUtenStillingberikelse = """
        {
          "stillingOpprettetTidspunkt": "2023-02-03T13:56:11.354599+01:00",
          "antallStillinger": 1,
          "erDirektemeldt": true,
          "organisasjonsnummer": "312113341",
          "kandidatlisteId": "fc63163c-96f9-491f-8c4e-e5bd0d35b463",
          "tidspunkt": "2023-02-20T12:41:13.303+01:00",
          "stillingsId": "fbf8c658-469a-44b8-8c0e-5f2013f1b835",
          "utførtAvNavIdent": "Z994241",
          "@event_name": "kandidat_v2.OpprettetEllerOppdaterteKandidatliste"
        }
    """.trimIndent()
}