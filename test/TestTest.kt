package no.nav.rekrutteringsbistand.statistikk

import com.github.kittinunf.fuel.httpGet
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals


class TestTest {

    private val basePath = "http://localhost:8080/rekrutteringsbistand-statistikk-api"

    @KtorExperimentalAPI
    companion object {
        init {
            main()
        }
    }

    @Test
    fun `GET til isReady skal returnere 'Ready'`() {
        val (_, _, result) = "$basePath/internal/isReady"
            .httpGet()
            .responseString()

        assertEquals("Ready", result.get())
    }

    @Test
    fun `GET til isAlive skal returnere 'Alive'`() {
        val (_, _, result) = "$basePath/internal/isAlive"
            .httpGet()
            .responseString()

        assertEquals("Alive", result.get())
    }

    // TODO: Finne ut hvorfor default HTTP-client ikke fungerer
//    @Test
//    fun `Ktor client skal også fungere`() = runBlocking {
//        val client = HttpClient()
//        val response = client.get<String>("$basePath/internal/isAlive")
//        println(response)
//        client.close()
//    }
}

