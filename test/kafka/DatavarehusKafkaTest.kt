package kafka

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import db.TestDatabase
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
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.After
import org.junit.Test
import randomPort
import start
import tilJson
import basePath
import java.time.Duration

@KtorExperimentalAPI
class DatavarehusKafkaTest {

    private val basePath = basePath(port)
    private val client = innloggaHttpClient()

    companion object {
        private val database = TestDatabase()
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment()
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(producerConfig(lokalKafka.brokersURL))

        init {
            start(database, port, datavarehusKafkaProducer)
            lokalKafka.start()
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)
        val consumer = KafkaConsumer<String, String>(consumerConfig(lokalKafka.brokersURL))
        consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))

        client.post<HttpResponse>("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }

        consumer.poll(Duration.ofSeconds(5))
            .map { melding -> jacksonObjectMapper().readValue<OpprettKandidatutfall>(melding.value()) }
            .forEachIndexed { index, melding ->
                assertThat(melding.aktørId).isEqualTo(kandidatutfallTilLagring[index].aktørId)
                assertThat(melding.utfall).isEqualTo(kandidatutfallTilLagring[index].utfall)
                assertThat(melding.navIdent).isEqualTo(kandidatutfallTilLagring[index].navIdent)
                assertThat(melding.navKontor).isEqualTo(kandidatutfallTilLagring[index].navKontor)
                assertThat(melding.kandidatlisteId).isEqualTo(kandidatutfallTilLagring[index].kandidatlisteId)
                assertThat(melding.stillingsId).isEqualTo(kandidatutfallTilLagring[index].stillingsId)
            }
    }

    @After
    fun cleanUp() {
        database.slettAlleUtfall()
        lokalKafka.tearDown()
    }
}
