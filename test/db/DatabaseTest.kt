package no.nav.rekrutteringsbistand.statistikk.db

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.rekrutteringsbistand.statistikk.start
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals

@KtorExperimentalAPI
class DatabaseTest {

    private val basePath = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache)

    companion object {
        private val port = Random.nextInt(1000, 9999)
        init {
            start(port)
        }
    }

    @Test
    fun `Skal kunne kjøre to tester om gangen`() = runBlocking {
        // Denne skal endres til skikkelig test
        val response: String = client.get("$basePath/internal/isReady")
        assertEquals("Ready", response)
    }
}
