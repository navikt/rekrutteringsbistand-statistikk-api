package no.nav.statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.request.*
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.rekrutteringsbistand.AvroKandidatutfall
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.SendtStatus.SENDT
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.After
import org.junit.AfterClass
import org.junit.Test
import no.nav.statistikkapi.*
import no.nav.statistikkapi.kandidatutfall.Kandidathendelselytter
import no.nav.statistikkapi.kandidatutfall.OpprettKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Utfall
import org.junit.Before
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Ignore

class DatavarehusKafkaTest {

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val utfall1 = etKandidatutfall.copy(tidspunktForHendelsen = nowOslo(), utfall = Utfall.PRESENTERT)
        val utfall2 = etKandidatutfall.copy(tidspunktForHendelsen = nowOslo().plusDays(1), utfall = Utfall.FATT_JOBBEN)
        val expected = listOf(utfall1, utfall2)
        expected.map(this@DatavarehusKafkaTest::tilKandidathendelseMap).map(objectMapper::writeValueAsString).forEach(rapid::sendTestMessage)

        val actuals: List<AvroKandidatutfall> = consumeKafka()

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


    fun tilKandidathendelseMap(opprettKandidatutfall: OpprettKandidatutfall) = when(opprettKandidatutfall.utfall) {
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
            )
        )
    }

    @Ignore
    @Test
    fun `Sending på Kafka-topic skal endre status fra IKKE_SENDT til SENDT`(): Unit = TODO()


    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
        mockOAuth2Server.shutdown()
    }

    companion object {
        private val database = TestDatabase()
        private val repository = TestRepository(database.dataSource)
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment(withSchemaRegistry = true)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(
            producerConfig(lokalKafka.brokersURL, lokalKafka.schemaRegistry!!.url)
        )
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)
        private val basePath = basePath(port)
        val rapid = TestRapid()

        private fun consumeKafka(): List<AvroKandidatutfall> {
            val consumer = KafkaConsumer<String, AvroKandidatutfall>(
                consumerConfig(
                    lokalKafka.brokersURL,
                    lokalKafka.schemaRegistry!!.url
                )
            )
            consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.topic))
            val records = consumer.poll(Duration.ofSeconds(5))
            return records.map { it.value() }
        }

        init {
            start(database, port, datavarehusKafkaProducer, mockOAuth2Server,rapid)
            lokalKafka.start()
        }

        @AfterClass
        @JvmStatic
        fun afterClassCleanup() {
            lokalKafka.tearDown()
        }

    }
}
