import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import db.TestDatabase
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
import no.nav.common.KafkaEnvironment
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import org.junit.Test
import java.time.Duration

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

        init {
            start(database, port, lokalKafka)
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        lokalKafka.adminClient?.deleteTopics(listOf(DatavarehusKafkaProducer.TOPIC))

        val consumer = KafkaConsumerUtils.opprettConsumer(lokalKafka.brokersURL)
        consumer.subscribe(listOf(DatavarehusKafkaProducer.TOPIC))


        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)

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

        lokalKafka.tearDown()
    }
}
