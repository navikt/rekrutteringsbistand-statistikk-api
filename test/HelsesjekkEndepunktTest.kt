import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.junit.Test


@KtorExperimentalAPI
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
        val response: String = client.get("$basePath/internal/isReady")
        assertThat(response).isEqualTo("Ready")
    }

    @Test
    fun `GET til isAlive skal returnere 'Alive'`() = runBlocking {
        val response: String = client.get("$basePath/internal/isAlive")
        assertThat(response).isEqualTo("Alive")
    }
}
