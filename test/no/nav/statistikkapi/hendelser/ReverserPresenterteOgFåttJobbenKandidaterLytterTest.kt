package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
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
        private val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)

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
    fun `mottak av kandidatutfall skal ikke registerers når utfall ikke finnes fra før`() {
        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)
        assertThat(testRepository.hentUtfall()).isEmpty()
    }

    @Test
    fun `mottak av kandidatutfall skal ikke registerers når utfall har feil status fra før`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )
        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)
        assertThat(testRepository.hentUtfall()).size().isEqualTo(1)

    }


    @Test
    fun `mottak av kandidatutfall skal være idempotent`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )
        assertThat(testRepository.hentUtfall()).size().isEqualTo(1)

        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)
        assertThat(testRepository.hentUtfall()).size().isEqualTo(2)

        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)
        assertThat(testRepository.hentUtfall()).size().isEqualTo(2)
    }


    @Test
    fun `Kan lagre kandidatutfall og stiling av FjernetRegistreringDeltCv-melding`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )

        rapid.sendTestMessage(fjernetRegistreringDeltCvMelding)

        val kandidatutfallFraDb = testRepository.hentUtfall()
        assertThat(kandidatutfallFraDb).hasSize(2)
        kandidatutfallFraDb[1].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(navIdent).isEqualTo("Z994633")
            assertThat(navKontor).isEqualTo("0313")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-16T10:38:25.877+01:00").toLocalDateTime())
            assertThat(synligKandidat).isNotNull().isTrue()
            assertThat(aktorId).isEqualTo("2258757075176")
            assertThat(alder).isEqualTo(55)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("BATT")
            assertThat(hovedmål).isEqualTo("SKAFFEA")

            assertThat(utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
        }

        val stillingFraDb = testRepository.hentStilling()
        assertThat(stillingFraDb).hasSize(1)
        assertThat(stillingFraDb[0].stillingskategori).isEqualTo(Stillingskategori.STILLING)
    }

    @Test
    fun `Vil lagre kandidatutfall og stilling av FjernetRegistreringDeltCv-melding dersom kategori er null`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )
        rapid.sendTestMessage(fjernetRegistreringDeltCvMeldingUtenStillingskategori)

        val kandidatutfallFraDb = testRepository.hentUtfall()
        assertThat(kandidatutfallFraDb).hasSize(2)


        val stillingFraDb = testRepository.hentStilling()
        assertThat(stillingFraDb).hasSize(1)
    }

    @Test
    fun `Vil ikke lagre kandidatutfall og stilling av FjernetRegistreringDeltCv-melding dersom stillingsinfo mangler`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.PRESENTERT,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )
        rapid.sendTestMessage(fjernetRegistreringDeltCvMeldingUtenStillingsinfo)

        val kandidatutfallFraDb = testRepository.hentUtfall()
        assertThat(kandidatutfallFraDb).hasSize(1)


        val stillingFraDb = testRepository.hentStilling()
        assertThat(stillingFraDb).isEmpty()
    }

    @Test
    fun `Kan opprette kandidatutfall av FjernetRegistreringFåttJobben-melding`() {
        kandidatutfallRepository.lagreUtfall(
            etKandidatutfall.copy(
                utfall = Utfall.FATT_JOBBEN,
                aktørId = "2258757075176",
                kandidatlisteId = "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
                stillingsId = "b5919e46-9882-4b3c-8089-53ad02f26023",
                innsatsbehov = "BATT",
                hovedmål = "SKAFFEA",
                alder = 55,
            )
        )

        rapid.sendTestMessage(fjernetRegistreringFåttJobbenMelding)

        val utfallFraDb = testRepository.hentUtfall()
        assertThat(utfallFraDb).hasSize(2)
        utfallFraDb[1].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(navIdent).isEqualTo("Z994633")
            assertThat(navKontor).isEqualTo("0313")
            assertThat(tidspunkt).isEqualTo(ZonedDateTime.parse("2023-02-16T10:38:25.877+01:00").toLocalDateTime())
            assertThat(synligKandidat).isNotNull().isTrue()
            assertThat(aktorId).isEqualTo("2258757075176")
            assertThat(alder).isEqualTo(55)
            assertThat(hullICv!!).isTrue()
            assertThat(innsatsbehov).isEqualTo("BATT")
            assertThat(hovedmål).isEqualTo("SKAFFEA")

            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
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
      "@event_name": "kandidat_v2.FjernetRegistreringDeltCv",
      "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b5919e46-9882-4b3c-8089-53ad02f26023",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          }
    }
""".trimIndent()

private val fjernetRegistreringDeltCvMeldingUtenStillingskategori = """
    {
      "aktørId": "2258757075176",
      "organisasjonsnummer": "312113341",
      "kandidatlisteId": "d5b5b4c1-0375-4719-9038-ab31fe27fb40",
      "tidspunkt": "2023-02-16T10:38:25.877+01:00",
      "stillingsId": "b5919e46-9882-4b3c-8089-53ad02f26023",
      "utførtAvNavIdent": "Z994633",
      "utførtAvNavKontorKode": "0313",
      "@event_name": "kandidat_v2.FjernetRegistreringDeltCv",
      "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b5919e46-9882-4b3c-8089-53ad02f26023",
            "eier": null,
            "notat": "sds",
            "stillingskategori": null
          }
    }
""".trimIndent()

private val fjernetRegistreringDeltCvMeldingUtenStillingsinfo = """
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
      "@event_name": "kandidat_v2.FjernetRegistreringFåttJobben",
      "stillingsinfo": {
            "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
            "stillingsid": "b5919e46-9882-4b3c-8089-53ad02f26023",
            "eier": null,
            "notat": "sds",
            "stillingskategori": "STILLING"
          }
    }
""".trimIndent()
