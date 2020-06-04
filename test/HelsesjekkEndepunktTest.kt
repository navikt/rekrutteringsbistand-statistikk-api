package no.nav.rekrutteringsbistand.statistikk

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals


@KtorExperimentalAPI
class HelsesjekkEndepunktTest {

    private val basePath = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache)

    companion object {
        private val port = Random.nextInt(1000, 9999)
        init {
            start(port)
        }
    }

    @Test
    fun `GET til isReady skal returnere 'Ready'`() = runBlocking {
        val response: String = client.get("$basePath/internal/isReady")
        assertEquals("Ready", response)
    }

    @Test
    fun `GET til isAlive skal returnere 'Alive'`() = runBlocking {
        val response: String = client.get("$basePath/internal/isAlive")
        assertEquals("Alive", response)
    }
}
