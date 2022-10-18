package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter.Type
import no.nav.statistikkapi.kandidatutfall.SendtStatus
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*

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
        val expectedUtfall = Type.valueOf(kandidathendelse["type"].toString()).toUtfall()
        assertThat(actual.utfall).isEqualTo(expectedUtfall)
        assertThat(actual.antallSendtForsøk).isEqualTo(0)
        assertThat(actual.sendtStatus).isEqualTo(SendtStatus.IKKE_SENDT)
        assertThat(actual.sisteSendtForsøk).isNull()
    }

    @Test
    fun `en melding om REGISTRER_CV_DELT lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.REGISTRER_CV_DELT)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.PRESENTERT)
    }

    @Test
    fun `en melding om CV_DELT_VIA_REKRUTTERINGSBISTAND lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.CV_DELT_VIA_REKRUTTERINGSBISTAND)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.PRESENTERT)
    }

    @Test
    fun `en melding om REGISTER_FÅTT_JOBBEN lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.REGISTRER_FÅTT_JOBBEN)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.FATT_JOBBEN)
    }

    @Test
    fun `en melding om FJERN_REGISTRERING_AV_CV_DELT lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.FJERN_REGISTRERING_AV_CV_DELT)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
    }

    @Test
    fun `en melding om FJERN_REGISTRERING_FÅTT_JOBBEN lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.FJERN_REGISTRERING_FÅTT_JOBBEN)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.PRESENTERT)
    }

    @Test
    fun `en melding om ANNULERT lagres i databasen`() {
        val kandidathendelsemelding =
            kandidathendelseMap(type = Type.ANNULLERT)

        val kandidathendelsesmeldingJson = objectMapper.writeValueAsString(kandidathendelsemelding)

        rapid.sendTestMessage(kandidathendelsesmeldingJson)

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().utfall).isEqualTo(Utfall.IKKE_PRESENTERT)
    }

    @Test
    fun `en kandidathendelsemelding skal ikke lagres dersom stillingId er null`() {
        val enMelding = kandidathendelseMap(stillingsId = null)

        rapid.sendTestMessage(objectMapper.writeValueAsString(enMelding))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(0)
    }

    // Dette vil gjelde for meldinger for usynlige kandidater
    @Test
    fun `en melding skal lagres selv når alder og hullICv er null`() {
        val enMelding = kandidathendelseMap(alder = null, hullICv = null)

        rapid.sendTestMessage(objectMapper.writeValueAsString(enMelding))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().hullICv).isNull()
        assertThat(alleUtfall.first().alder).isNull()
    }

    @Test
    fun `en melding skal lagres selv når navKontorKode er tom streng`() {
        val enMelding = kandidathendelseMap(type = Type.ANNULLERT, utførtAvNavKontorKode = "")

        rapid.sendTestMessage(objectMapper.writeValueAsString(enMelding))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().navKontor).isEmpty()
    }

    @Test
    fun `En melding skal ikke lagres dersom utfall er lik som på siste melding for samme kandidat og kandidatliste`() {
        val etTidspunkt = nowOslo().minusHours(2)
        val enMelding = kandidathendelseMap(tidspunkt = etTidspunkt.toString())
        val etSenereTidspunkt = nowOslo()
        val enLikMeldingMenMedSenereTidspunkt = kandidathendelseMap(tidspunkt = etSenereTidspunkt.toString())

        rapid.sendTestMessage(objectMapper.writeValueAsString(enMelding))
        rapid.sendTestMessage(objectMapper.writeValueAsString(enLikMeldingMenMedSenereTidspunkt))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(1)
        assertThat(alleUtfall.first().tidspunkt).isEqualTo(etTidspunkt.toLocalDateTime())
    }

    @Test
    fun `Skal ikke lagre duplikat`() {
        val enPresentertMelding =
            kandidathendelseMap(tidspunkt = nowOslo().minusDays(2).toString(), type = Type.REGISTRER_CV_DELT)
        val enFåttJobbenMelding =
            kandidathendelseMap(tidspunkt = nowOslo().minusDays(1).toString(), type = Type.REGISTRER_FÅTT_JOBBEN)
        val duplikat = enPresentertMelding

        rapid.sendTestMessage(objectMapper.writeValueAsString(enPresentertMelding))
        rapid.sendTestMessage(objectMapper.writeValueAsString(enFåttJobbenMelding))
        rapid.sendTestMessage(objectMapper.writeValueAsString(duplikat))

        val alleUtfall = testRepository.hentUtfall()
        assertThat(alleUtfall).hasSize(2)
        assertThat(alleUtfall.map { it.utfall }).containsExactlyInAnyOrder(Utfall.PRESENTERT, Utfall.FATT_JOBBEN)
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

    fun kandidathendelseMap(
        tidspunkt: String = "2022-09-18T10:33:02.5+02:00",
        type: Type = Type.CV_DELT_VIA_REKRUTTERINGSBISTAND,
        stillingsId: String? = "b3c925af-ebf4-50d1-aeee-efc9259107a4",
        alder: Int? = 62,
        hullICv: Boolean? = true,
        utførtAvNavKontorKode: String = "0313"
    ) = mapOf(
        "@event_name" to "kandidat.${type.eventName}",
        "kandidathendelse" to mapOf(
            "type" to "${type.name}",
            "aktørId" to "dummyAktørid",
            "organisasjonsnummer" to "123456789",
            "kandidatlisteId" to "24e81692-37ef-4fda-9b55-e17588f65061",
            "tidspunkt" to "$tidspunkt",
            "stillingsId" to stillingsId,
            "utførtAvNavIdent" to "Z994632",
            "utførtAvNavKontorKode" to utførtAvNavKontorKode,
            "synligKandidat" to true,
            "harHullICv" to hullICv,
            "alder" to alder,
            "tilretteleggingsbehov" to listOf("arbeidstid")
        )
    ) +
            if (stillingsId == null) emptyArray() else
                arrayOf(
                    "stilling" to mapOf(
                        "stillingsinfoid" to UUID.randomUUID().toString(),
                        "stillingsid" to stillingsId,
                        "eier" to mapOf(
                            "navident" to "A123456",
                            "navn" to "Navnesen"
                        ),
                        "stillingskategori" to "STILLING"
                    )
                )
}