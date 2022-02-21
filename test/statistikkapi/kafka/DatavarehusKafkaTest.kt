package statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import statistikkapi.basePath
import statistikkapi.db.TestDatabase
import statistikkapi.db.TestRepository
import statistikkapi.etKandidatutfall
import statistikkapi.innloggaHttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.rekrutteringsbistand.AvroKandidatutfall
import statistikkapi.kandidatutfall.Kandidatutfall
import statistikkapi.kandidatutfall.SendtStatus.SENDT
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.After
import org.junit.AfterClass
import org.junit.Test
import statistikkapi.randomPort
import statistikkapi.start
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class DatavarehusKafkaTest {


    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val expected = listOf(etKandidatutfall, etKandidatutfall)

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = expected
        }

        val actuals: List<AvroKandidatutfall> = consumeKafka()

        assertThat(actuals.count()).isEqualTo(2)
        actuals.forEachIndexed { index, actual ->
            assertThat(actual.getAktørId()).isEqualTo(expected[index].aktørId)
            assertThat(actual.getUtfall()).isEqualTo(expected[index].utfall.name)
            assertThat(actual.getNavIdent()).isEqualTo(expected[index].navIdent)
            assertThat(actual.getNavKontor()).isEqualTo(expected[index].navKontor)
            assertThat(actual.getKandidatlisteId()).isEqualTo(expected[index].kandidatlisteId)
            assertThat(actual.getStillingsId()).isEqualTo(expected[index].stillingsId)
            assertThat(LocalDateTime.parse(actual.getTidspunkt())).isBetween(now().minusSeconds(10), now())
        }
    }

    @Test
    fun `Sending på Kafka-topic skal endre status fra IKKE_SENDT til SENDT`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = kandidatutfallTilLagring
        }
        consumeKafka() // Vent

        val now = now()
        val actuals: List<Kandidatutfall> = repository.hentUtfall()
        actuals.forEach {
            assertThat(it.sendtStatus).isEqualTo(SENDT)
            assertThat(it.antallSendtForsøk).isEqualTo(1)
            assertThat(it.sisteSendtForsøk).isNotNull()
            assertThat(it.sisteSendtForsøk!!).isBetween(now.minusSeconds(10), now)
        }
    }


    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
    }


    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabase()
        private val repository = TestRepository(database.dataSource)
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment(withSchemaRegistry = true)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(
            producerConfig(lokalKafka.brokersURL, lokalKafka.schemaRegistry!!.url)
        )

        private fun consumeKafka(): List<AvroKandidatutfall> {
            val consumer = KafkaConsumer<String, AvroKandidatutfall>(
                consumerConfig(
                    lokalKafka.brokersURL,
                    lokalKafka.schemaRegistry!!.url
                )
            )
            consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))
            val records = consumer.poll(Duration.ofSeconds(5))
            return records.map { it.value() }
        }

        init {
            start(database, port, datavarehusKafkaProducer)
            lokalKafka.start()
        }

        @AfterClass
        @JvmStatic
        fun afterClassCleanup() {
            lokalKafka.tearDown()
        }

    }
}
