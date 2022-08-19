package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.kandidatutfall.SendtStatus
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime

class LagreStatistikkTest {

    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val testRepository = TestRepository(database.dataSource)

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid)
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleUtfall()
        rapid.reset()
    }

    @Test
    fun test() {
        val json = "{}"
        rapid.sendTestMessage(json)
    }

    @Test
    fun `en kandidathendelsemelding skal lagres som kandidatutfall i databasen`() {
        val kandidathendelsemelding = kandidathendelseMap()
        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall.size).isEqualTo(1)

        val actual = alleUtfall.first()
        val kandidathendelse = kandidathendelsemelding["kandidathendelse"] as Map<*, *>
        assertThat(actual.dbId).isNotNull()
        assertThat(actual.aktorId).isEqualTo(kandidathendelse["aktørId"])
        assertThat(actual.alder).isEqualTo(kandidathendelse["alder"])
        val expectedTidspunkt = ZonedDateTime.parse(kandidathendelse["tidspunkt"].toString()).toLocalDateTime()
        assertThat(actual.tidspunkt).isEqualTo(expectedTidspunkt)
        assertThat(actual.hullICv).isEqualTo(kandidathendelse["harHullICv"])
        assertThat(actual.kandidatlisteId.toString()).isEqualTo(kandidathendelse["kandidatlisteId"])
        assertThat(actual.stillingsId.toString()).isEqualTo(kandidathendelse["stillingsId"])
        assertThat(actual.navIdent).isEqualTo(kandidathendelse["utførtAvNavIdent"])
        assertThat(actual.navKontor).isEqualTo(kandidathendelse["utførtAvNavKontorKode"])
        assertThat(actual.tilretteleggingsbehov).isEqualTo(kandidathendelse["tilretteleggingsbehov"])
        assertThat(actual.synligKandidat).isEqualTo(kandidathendelse["synligKandidat"])
        val expectedUtfall = Kandidathendelselytter.Type.valueOf(kandidathendelse["type"].toString()).toUtfall()
        assertThat(actual.utfall).isEqualTo(expectedUtfall)
        assertThat(actual.antallSendtForsøk).isEqualTo(0)
        assertThat(actual.sendtStatus).isEqualTo(SendtStatus.IKKE_SENDT)
        assertThat(actual.sisteSendtForsøk).isNull()
    }

    @Test
    fun `en kandidathendelsemelding skal ikke lagres som kandidatutfall når samme utfall allerede er lagret`() {
        val enMelding = kandidathendelseMap()
        val enHeltLikMelding = kandidathendelseMap()

        rapid.sendTestMessage(objectMapper.writeValueAsString(enMelding))
        rapid.sendTestMessage(objectMapper.writeValueAsString(enHeltLikMelding))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
    }

    @Test
    fun `en kandidathendelsemelding skal lagres om hendelsestidspunkt er etter spesifisert tidspunkt`() {
        val kandidathendelsemelding = kandidathendelseMap(tidspunkt = "2022-08-19T11:00:01+02:00")
        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall.size).isEqualTo(1)
    }

    @Test
    fun `en kandidathendelsemelding skal ikke lagres om hendelsestidspunkt er lik spesifisert tidspunkt`() {
        val kandidathendelsemelding = kandidathendelseMap(tidspunkt = "2022-08-19T11:00:00+02:00")
        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).isEmpty()
    }


    fun kandidathendelseMap(tidspunkt: String = "2022-09-18T10:33:02.5+02:00") = mapOf(
        "@event_name" to "kandidat.cv-delt-med-arbeidsgiver-via-rekrutteringsbistand",
        "kandidathendelse" to mapOf(
            "type" to "CV_DELT_VIA_REKRUTTERINGSBISTAND",
            "aktørId" to "dummyAktørid",
            "organisasjonsnummer" to "123456789",
            "kandidatlisteId" to "24e81692-37ef-4fda-9b55-e17588f65061",
            "tidspunkt" to "$tidspunkt",
            "stillingsId" to "b3c925af-ebf4-50d1-aeee-efc9259107a4",
            "utførtAvNavIdent" to "Z994632",
            "utførtAvNavKontorKode" to "0313",
            "synligKandidat" to true,
            "harHullICv" to true,
            "alder" to 62,
            "tilretteleggingsbehov" to listOf("arbeidstid")
        )
    )
}
