package no.nav.statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotlinx.coroutines.runBlocking
import no.nav.rekrutteringsbistand.AvroKandidatutfallV2
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.*
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.SendtStatus
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class DatavarehusKafkaTest {

    @Test
    fun `Kandidatutfall hendelse skal produsere melding på Kafka-topic`() = runBlocking {
        val rekrutteringstreffId = UUID.randomUUID()
        val utfall1 = etKandidatutfall.copy(tidspunktForHendelsen = nowOslo(), utfall = Utfall.PRESENTERT)
        val utfall2 = etKandidatutfall.copy(
            tidspunktForHendelsen = nowOslo().plusDays(1),
            utfall = Utfall.FATT_JOBBEN,
            stillingsId = UUID.randomUUID().toString(),
            rekrutteringstreffId = rekrutteringstreffId.toString(),
        )


        val expected = listOf(utfall1, utfall2)
        val forventedeStillingskategorier = listOf(Stillingskategori.STILLING, Stillingskategori.REKRUTTERINGSTREFF)

        expected.forEachIndexed { index, it ->
            stillingRepository.lagreStilling(it.stillingsId, forventedeStillingskategorier[index])
            kandidatutfallRepository.lagreUtfall(it)
        }

        testHentUsendteUtfallOgSendPåKafka.run()

        val actualRecords = mockProducer.history()
        val actuals: List<AvroKandidatutfallV2> = actualRecords.map { it.value() }

        assertThat(actuals.count()).isEqualTo(2)
        actuals.forEachIndexed { index, actual ->
            assertThat(actualRecords[index].topic()).isEqualTo(DatavarehusKafkaProducerImpl.topic)
            assertThat(actual.getAktørId()).isEqualTo(expected[index].aktørId)
            assertThat(actual.getUtfall()).isEqualTo(expected[index].utfall.name)
            assertThat(actual.getNavIdent()).isEqualTo(expected[index].navIdent)
            assertThat(actual.getNavKontor()).isEqualTo(expected[index].navKontor)
            assertThat(actual.getKandidatlisteId()).isEqualTo(expected[index].kandidatlisteId)
            assertThat(actual.getStillingsId()).isEqualTo(expected[index].stillingsId)
            assertThat(actual.getRekrutteringstreffId()).isEqualTo(expected[index].rekrutteringstreffId)
            assertThat(actual.getStillingskategori()).isEqualTo(forventedeStillingskategorier[index].name)

            val expectedTidspunkt = if (index == 0) utfall1.tidspunktForHendelsen else utfall2.tidspunktForHendelsen
            assertThat(LocalDateTime.parse(actual.getTidspunkt())).isBetween(
                expectedTidspunkt.toLocalDateTime().minusSeconds(10), expectedTidspunkt.toLocalDateTime()
            )
        }
    }

    @Test
    fun `Sending på Kafka-topic skal endre status fra IKKE_SENDT til SENDT`() = runBlocking {
        kandidatutfallRepository.lagreUtfall(etKandidatutfall)
        stillingRepository.lagreStilling(enStillingsId.toString(), Stillingskategori.STILLING)
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
        private val stillingRepository = StillingRepository(database.dataSource)
        private val dummyAvroKandidatutfallSerializer = { _: String, _: AvroKandidatutfallV2 -> ByteArray(0) }
        private val mockProducer = MockProducer(true, null, StringSerializer(), dummyAvroKandidatutfallSerializer)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(mockProducer)
        private val testHentUsendteUtfallOgSendPåKafka =
            hentUsendteUtfallOgSendPåKafka(
                kandidatutfallRepository,
                datavarehusKafkaProducer,
                StillingRepository(database.dataSource)
            )
        private val mockOAuth2Server = MockOAuth2Server()
        val rapid = TestRapid()

        init {
            start(database, port, mockOAuth2Server, rapid)
        }
    }
}
