package no.nav.statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.rekrutteringsbistand.AvroKandidatutfall
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.*
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.*
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.ElasticSearchStilling
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingService
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class DatavarehusKafkaTest {

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val utfall1 = etKandidatutfall.copy(tidspunktForHendelsen = nowOslo(), utfall = Utfall.PRESENTERT)
        val utfall2 = etKandidatutfall.copy(tidspunktForHendelsen = nowOslo().plusDays(1), utfall = Utfall.FATT_JOBBEN)
        val expected = listOf(utfall1, utfall2)
        expected.map(this@DatavarehusKafkaTest::tilKandidathendelseMap).map(objectMapper::writeValueAsString)
            .forEach(rapid::sendTestMessage)

        testHentUsendteUtfallOgSendPåKafka.run()

        val actuals: List<AvroKandidatutfall> = mockProducer.history().map { it.value() }

        assertThat(actuals.count()).isEqualTo(2)
        actuals.forEachIndexed { index, actual ->
            assertThat(actual.getAktørId()).isEqualTo(expected[index].aktørId)
            assertThat(actual.getUtfall()).isEqualTo(expected[index].utfall.name)
            assertThat(actual.getNavIdent()).isEqualTo(expected[index].navIdent)
            assertThat(actual.getNavKontor()).isEqualTo(expected[index].navKontor)
            assertThat(actual.getKandidatlisteId()).isEqualTo(expected[index].kandidatlisteId)
            assertThat(actual.getStillingsId()).isEqualTo(expected[index].stillingsId)

            val expectedTidspunkt = if (index == 0) utfall1.tidspunktForHendelsen else utfall2.tidspunktForHendelsen
            assertThat(LocalDateTime.parse(actual.getTidspunkt())).isBetween(
                expectedTidspunkt.toLocalDateTime().minusSeconds(10), expectedTidspunkt.toLocalDateTime()
            )
        }
    }


    fun tilKandidathendelseMap(opprettKandidatutfall: OpprettKandidatutfall) = when (opprettKandidatutfall.utfall) {
        Utfall.FATT_JOBBEN -> Kandidathendelselytter.Type.REGISTRER_FÅTT_JOBBEN
        Utfall.IKKE_PRESENTERT -> Kandidathendelselytter.Type.ANNULLERT
        Utfall.PRESENTERT -> Kandidathendelselytter.Type.REGISTRER_CV_DELT
    }.let { type ->
        mapOf(
            "@event_name" to "kandidat.${type.eventName}",
            "kandidathendelse" to mapOf(
                "type" to "${type.name}",
                "aktørId" to opprettKandidatutfall.aktørId,
                "organisasjonsnummer" to "123456789",
                "kandidatlisteId" to opprettKandidatutfall.kandidatlisteId,
                "tidspunkt" to "${opprettKandidatutfall.tidspunktForHendelsen}",
                "stillingsId" to opprettKandidatutfall.stillingsId,
                "utførtAvNavIdent" to opprettKandidatutfall.navIdent,
                "utførtAvNavKontorKode" to opprettKandidatutfall.navKontor,
                "synligKandidat" to true,
                "harHullICv" to opprettKandidatutfall.harHullICv,
                "alder" to opprettKandidatutfall.alder,
                "tilretteleggingsbehov" to opprettKandidatutfall.tilretteleggingsbehov
            ),
            "stillingsinfo" to mapOf(
                "stillingsinfoid" to UUID.randomUUID().toString(),
                "stillingsid" to opprettKandidatutfall.stillingsId,
                "eier" to mapOf(
                    "navident" to "A123456",
                    "navn" to "Navnesen"
                ),
                "stillingskategori" to "STILLING"
            )
        )
    }

    @Test
    fun `Sending på Kafka-topic skal endre status fra IKKE_SENDT til SENDT`() = runBlocking {
        kandidatutfallRepository.lagreUtfall(etKandidatutfall)
        val oppsett: List<Kandidatutfall> = repository.hentUtfall()
        assertThat(oppsett.count()).isEqualTo(1)
        oppsett.forEach {
            assertThat(it.sendtStatus).isEqualTo(SendtStatus.IKKE_SENDT)
            assertThat(it.antallSendtForsøk).isEqualTo(0)
            assertThat(it.sisteSendtForsøk).isNull()
        }

        testHentUsendteUtfallOgSendPåKafka.run()

        val now = LocalDateTime.now()
        val actuals: List<Kandidatutfall> = repository.hentUtfall()

        assertThat(actuals.count()).isEqualTo(1)
        actuals.forEach {
            assertThat(it.sendtStatus).isEqualTo(SendtStatus.SENDT)
            assertThat(it.antallSendtForsøk).isEqualTo(1)
            assertThat(it.sisteSendtForsøk).isNotNull()
            assertThat(it.sisteSendtForsøk!!).isBetween(now.minusSeconds(10), now)
        }
    }


    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
        mockOAuth2Server.shutdown()
    }

    companion object {
        private val database = TestDatabase()
        private val repository = TestRepository(database.dataSource)
        private val port = randomPort()
        private val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
        private val dummyAvroKandidatutfallSerializer = { _: String, _: AvroKandidatutfall -> ByteArray(0) }
        private val mockProducer = MockProducer(true, StringSerializer(), dummyAvroKandidatutfallSerializer)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(mockProducer)
        private val elasticSearchKlient = object : ElasticSearchKlient {
            override fun hentStilling(stillingUuid: String): ElasticSearchStilling = enElasticSearchStilling()
        }
        private val testHentUsendteUtfallOgSendPåKafka =
            hentUsendteUtfallOgSendPåKafka(
                kandidatutfallRepository,
                datavarehusKafkaProducer,
                StillingService(elasticSearchKlient, StillingRepository(database.dataSource))
            )
        private val mockOAuth2Server = MockOAuth2Server()
        val rapid = TestRapid()

        init {
            start(database, port, mockOAuth2Server, rapid)
        }
    }
}
