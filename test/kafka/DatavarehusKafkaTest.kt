package kafka

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import db.TestDatabase
import etKandidatutfall
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.cookies.ConstantCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import lagCookie
import no.nav.common.KafkaEnvironment
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducerImpl
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.log
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.After
import org.junit.Test
import randomPort
import start
import tilJson
import java.time.Duration
import java.util.*

@KtorExperimentalAPI
class DatavarehusKafkaTest {

    private val basePath = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache) {
        install(HttpCookies) {
            storage = ConstantCookiesStorage(lagCookie())
        }
    }

    companion object {
        private val database = TestDatabase()
        private val port = randomPort()
        private val lokalKafka = KafkaEnvironment()
        private val datavarehusKafkaProducer = DatavarehusKafkaProducerImpl(lokalKafka.brokersURL)

        init {
            start(database, port, datavarehusKafkaProducer)
            lokalKafka.start()
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)
        val consumer = opprettConsumer(lokalKafka.brokersURL)
        consumer.subscribe(listOf(DatavarehusKafkaProducerImpl.TOPIC))

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
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

    private fun opprettConsumer(bootstrapServers: String): KafkaConsumer<String, String> {
        val consumerConfig: Properties = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.GROUP_ID_CONFIG, "mingroupid")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }
        return KafkaConsumer(consumerConfig)
    }

    @After
    fun cleanUp() {
        database.slettAlleUtfall()
        lokalKafka.tearDown()
    }
}
