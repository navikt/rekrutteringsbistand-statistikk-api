package kafka

import assertk.assertThat
import assertk.assertions.isEqualTo
import basePath
import db.TestDatabaseImpl
import db.TestRepository
import etKandidatutfall
import innloggaHttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.rekrutteringsbistand.KandidatUtfall
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.SENDT
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerImpl
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.After
import org.junit.AfterClass
import org.junit.Test
import randomPort
import start
import tilJson
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@KtorExperimentalAPI
class DatavarehusKafkaTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabaseImpl()
        private val repository = TestRepository(database.dataSource)
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment(withSchemaRegistry = true)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(
            producerConfig(lokalKafka.brokersURL, lokalKafka.schemaRegistry!!.url)
        )

        init {
            start(database, port, datavarehusKafkaProducer)
            lokalKafka.start()
        }

        @AfterClass
        fun afterClassCleanup() {
            lokalKafka.tearDown()
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)
        val consumer = KafkaConsumer<String, KandidatUtfall>(
            consumerConfig(
                lokalKafka.brokersURL,
                lokalKafka.schemaRegistry!!.url
            )
        )
        consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }

        val meldinger = consumer.poll(Duration.ofSeconds(5))

        assertThat(meldinger.count()).isEqualTo(2)
        meldinger
            .map { it.value() }
            .forEachIndexed { index, melding ->
                assertThat(melding.getAktørId()).isEqualTo(kandidatutfallTilLagring[index].aktørId)
                assertThat(melding.getUtfall()).isEqualTo(kandidatutfallTilLagring[index].utfall)
                assertThat(melding.getNavIdent()).isEqualTo(kandidatutfallTilLagring[index].navIdent)
                assertThat(melding.getNavKontor()).isEqualTo(kandidatutfallTilLagring[index].navKontor)
                assertThat(melding.getKandidatlisteId()).isEqualTo(kandidatutfallTilLagring[index].kandidatlisteId)
                assertThat(melding.getStillingsId()).isEqualTo(kandidatutfallTilLagring[index].stillingsId)
            }
    }

    @Test
    fun `Sending på Kafka-topic skal endre status fra IKKE_SENDT til SENDT`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)

        val consumer = KafkaConsumer<String, KandidatUtfall>(
            consumerConfig(
                lokalKafka.brokersURL,
                lokalKafka.schemaRegistry!!.url
            )
        )
        consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }
        consumer.poll(Duration.ofSeconds(5)) // Vent

        val actual = repository.hentUtfall()
        actual.forEach {
            assertThat(it.sendtStatus).isEqualTo(SENDT)
            assertThat(it.antallSendtForsøk).isEqualTo(1)
            assertThat(it.sisteSendtForsøk!!.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            )
        }
    }

    @After
    fun cleanUp() {
        repository.slettAlleUtfall()
    }


}
