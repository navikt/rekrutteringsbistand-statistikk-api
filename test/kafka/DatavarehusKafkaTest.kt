package kafka

import assertk.assertThat
import assertk.assertions.isEqualTo
import db.TestDatabaseImpl
import etKandidatutfall
import innloggaHttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerImpl
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.After
import org.junit.Test
import randomPort
import start
import tilJson
import basePath
import no.nav.rekrutteringsbistand.KandidatUtfall
import java.time.Duration

@KtorExperimentalAPI
class DatavarehusKafkaTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabaseImpl()
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment(withSchemaRegistry = true)
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(
            producerConfig(lokalKafka.brokersURL, lokalKafka.schemaRegistry!!.url)
        )

        init {
            start(database, port, datavarehusKafkaProducer)
            lokalKafka.start()
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)
        val consumer = KafkaConsumer<String, KandidatUtfall>(consumerConfig(lokalKafka.brokersURL, lokalKafka.schemaRegistry!!.url))
        consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }

        consumer.poll(Duration.ofSeconds(5))
            .map { melding -> melding.value() }
            .forEachIndexed { index, melding ->
                assertThat(melding.getAktørId()).isEqualTo(kandidatutfallTilLagring[index].aktørId)
                assertThat(melding.getUtfall()).isEqualTo(kandidatutfallTilLagring[index].utfall)
                assertThat(melding.getNavIdent()).isEqualTo(kandidatutfallTilLagring[index].navIdent)
                assertThat(melding.getNavKontor()).isEqualTo(kandidatutfallTilLagring[index].navKontor)
                assertThat(melding.getKandidatlisteId()).isEqualTo(kandidatutfallTilLagring[index].kandidatlisteId)
                assertThat(melding.getStillingsId()).isEqualTo(kandidatutfallTilLagring[index].stillingsId)
            }
    }

    @After
    fun cleanUp() {
        database.slettAlleUtfall()
        lokalKafka.tearDown()
    }
}
