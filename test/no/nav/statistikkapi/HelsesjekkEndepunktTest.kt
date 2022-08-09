package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HelsesjekkEndepunktTest {

    private val basePath = basePath(port)
    private val client = HttpClient(Apache)

    companion object {
        private val port = randomPort()
        init {
            start(port = port)
        }
    }

    @Test
    fun `GET til isReady skal returnere 'Ready'`() = runBlocking {
        val response: String = client.get("$basePath/internal/isReady").body()
        assertThat(response).isEqualTo(""""Ready"""")
    }

    @Test
    fun `GET til isAlive skal returnere 'Alive'`() = runBlocking {
        val response: String = client.get("$basePath/internal/isAlive").body()
        assertThat(response).isEqualTo(""""Alive"""")
    }
}
