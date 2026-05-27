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
import kotlin.test.assertFailsWith

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

    @Test
    fun `Kan opprette kandidatutfall med rekrutteringstreffId for FåttJobben`() {
        rapid.sendTestMessage(registrertFåttJobbenMeldingMedRekrutteringstreffId)

        val utfallFraDb = testRepository.hentUtfall()
        val stillingFraDb = testRepository.hentStilling()
        assertThat(utfallFraDb).hasSize(1)
        assertThat(stillingFraDb).hasSize(1)
        utfallFraDb[0].apply {
            assertThat(utfall).isEqualTo(Utfall.FATT_JOBBEN)
            assertThat(rekrutteringstreffId).isEqualTo(UUID.fromString(etRekrutteringstreffId))
        }
        stillingFraDb[0].apply {
            assertThat(stillingskategori).isEqualTo(Stillingskategori.REKRUTTERINGSTREFF)
        }
    }

    @Test
    fun `Ugyldig rekrutteringstreffId kaster exception`() {
        assertFailsWith<IllegalArgumentException> {
            rapid.sendTestMessage(registrertFåttJobbenMeldingMedUgyldigRekrutteringstreffId)
        }

        assertThat(testRepository.hentUtfall()).hasSize(0)
    }
}

private const val etRekrutteringstreffId = "2cb83e65-4fd3-4a62-bdc7-64d4a9fc335c"

private val standardInkludering = """
    {
      "harHullICv": true,
      "alder": 53,
      "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
      "hovedmål": "SKAFFEA"
    }
""".trimIndent()

private val standardStillingsinfo = """
    {
      "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
      "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
      "eier": null,
      "notat": "sds",
      "stillingskategori": "STILLING"
    }
""".trimIndent()

private val standardStilling = """
    {
      "stillingstittel": "ergerg",
      "erDirektemeldt": true,
      "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
      "antallStillinger": 3,
      "organisasjonsnummer": "923282556",
      "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
    }
""".trimIndent()

private fun byggMelding(
    eventName: String = "kandidat_v2.RegistrertDeltCv",
    tidspunkt: String = "2023-02-13T09:57:34.643+01:00",
    stillingsId: String? = "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
    synligKandidat: Boolean = true,
    inkludering: String? = standardInkludering,
    stillingsinfo: String? = standardStillingsinfo,
    stilling: String? = standardStilling,
): String {
    val stillingsinfoBlokk = stillingsinfo?.let {
        """
            ,
              "stillingsinfo": $it
        """.trimIndent()
    } ?: ""
    val stillingBlokk = stilling?.let {
        """
            ,
              "stilling": $it
        """.trimIndent()
    } ?: ""
    return """
        {
          "aktørId": "2133747575903",
          "organisasjonsnummer": "894822082",
          "kandidatlisteId": "6e22ced0-241b-4889-8285-7ca268d91b8d",
          "tidspunkt": "$tidspunkt",
          "stillingsId": ${stillingsId?.let { "\"$it\"" } ?: "null"},
          "utførtAvNavIdent": "Z990281",
          "utførtAvNavKontorKode": "0314",
          "synligKandidat": $synligKandidat,
          "inkludering": ${inkludering ?: "null"},
          "@event_name": "$eventName"$stillingsinfoBlokk$stillingBlokk
        }
    """.trimIndent()
}

private fun registrertDeltCvmelding(
    tidspunkt: ZonedDateTime = ZonedDateTime
        .parse("2023-02-13T09:57:34.643+01:00")
        .withZoneSameInstant(ZoneId.of("Europe/Oslo"))
) = byggMelding(tidspunkt = tidspunkt.toString())

private fun registrertDeltCvMeldingMedDeprecatedTilretteleggingsbehov(
    tidspunkt: ZonedDateTime = ZonedDateTime
        .parse("2023-02-13T09:57:34.643+01:00")
        .withZoneSameInstant(ZoneId.of("Europe/Oslo"))
) = byggMelding(
    tidspunkt = tidspunkt.toString(),
    inkludering = """
        {
          "harHullICv": true,
          "alder": 53,
          "tilretteleggingsbehov": ["arbeidstid"],
          "innsatsbehov": "SPESIELT_TILPASSET_INNSATS",
          "hovedmål": "SKAFFEA"
        }
    """.trimIndent()
)


private val registrertDeltCvmeldingUsynligKandidatUtenInkludering = byggMelding(
    synligKandidat = false,
    inkludering = null
)

private val registrertDeltCvmeldingMedNullverdier = byggMelding(
    stillingsId = null,
    inkludering = null,
    stillingsinfo = null,
    stilling = null
)

private val registrerDeltCVMeldingUtenStillingberikelse = byggMelding(
    stillingsinfo = null,
    stilling = null
)

private val registrertFåttJobbenMelding = byggMelding(
    eventName = "kandidat_v2.RegistrertFåttJobben",
    tidspunkt = "2023-02-13T12:39:52.205+01:00"
)

val registrertFåttJobbenMeldingUtenStillingberikelse = byggMelding(
    eventName = "kandidat_v2.RegistrertFåttJobben",
    tidspunkt = "2023-02-13T10:03:02.145+01:00",
    stillingsinfo = null,
    stilling = null
)

private val standardRekrutteringstreffStilling = """
    {
      "stillingstittel": "Rekrutteringstreff",
      "erDirektemeldt": true,
      "stillingOpprettetTidspunkt": "2022-04-11T14:32:47.215151+02:00[Europe/Oslo]",
      "antallStillinger": 1,
      "organisasjonsnummer": "923282556",
      "stillingensPubliseringstidspunkt": "2022-04-12T01:00:00.000000+02:00[Europe/Oslo]"
    }
""".trimIndent()

private val registrertFåttJobbenMeldingMedRekrutteringstreffId = byggMelding(
    eventName = "kandidat_v2.RegistrertFåttJobben",
    tidspunkt = "2023-02-13T12:39:52.205+01:00",
    stillingsinfo = """
        {
          "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
          "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
          "eier": null,
          "notat": "sds",
          "stillingskategori": "REKRUTTERINGSTREFF",
          "rekrutteringstreffId": "$etRekrutteringstreffId"
        }
    """.trimIndent(),
    stilling = standardRekrutteringstreffStilling,
)

private val registrertFåttJobbenMeldingMedUgyldigRekrutteringstreffId = byggMelding(
    eventName = "kandidat_v2.RegistrertFåttJobben",
    tidspunkt = "2023-02-13T12:39:52.205+01:00",
    stillingsinfo = """
        {
          "stillingsinfoid": "88cdcd85-aa9d-4166-84b9-1567e089e5cc",
          "stillingsid": "b2d427a4-061c-4ba4-890b-b7b0e04fb000",
          "eier": null,
          "notat": "sds",
          "stillingskategori": "REKRUTTERINGSTREFF",
          "rekrutteringstreffId": "ikke-en-uuid"
        }
    """.trimIndent(),
    stilling = standardRekrutteringstreffStilling,
)
