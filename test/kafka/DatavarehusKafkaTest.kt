package kafka

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
import io.mockk.*
import kotlinx.coroutines.runBlocking
import lagCookie
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import org.junit.Test
import randomPort
import start
import tilJson

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
        private val datavarehusKafkaProducer = mockk<DatavarehusKafkaProducer>()

        init {
            start(database, port, datavarehusKafkaProducer)
        }
    }

    @Test
    fun `POST til kandidatutfall skal produsere melding på Kafka-topic`() = runBlocking {
        val kandidatutfallTilLagring = listOf(etKandidatutfall, etKandidatutfall)
        every { datavarehusKafkaProducer.send(any()) } returns Unit

        val response: HttpResponse = client.post("$basePath/kandidatutfall") {
            body = TextContent(tilJson(kandidatutfallTilLagring), ContentType.Application.Json)
        }

        kandidatutfallTilLagring.forEach { verify { datavarehusKafkaProducer.send(it) } }
    }
}
