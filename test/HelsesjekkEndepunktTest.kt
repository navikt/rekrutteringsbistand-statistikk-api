import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals


@KtorExperimentalAPI
class HelsesjekkEndepunktTest {

    private val basePath = "http://localhost:$port/rekrutteringsbistand-statistikk-api"
    private val client = HttpClient(Apache)

    companion object {
        private val port = randomPort()
        init {
            start(port = port)
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
