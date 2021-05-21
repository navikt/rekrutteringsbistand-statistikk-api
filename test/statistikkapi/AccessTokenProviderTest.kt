package statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import org.junit.Test

class AccessTokenProviderTest {

    @Test
    fun verifiserAtTokenKunHentesEnGang() {
        var antallGangerNyTokenHentet = 0
        val tokenClient = AccessTokenProvider(config = config(), httpKlient = lagVerifiableHttpClient(123) {antallGangerNyTokenHentet++})
        tokenClient.getBearerToken("scope")
        assertThat(antallGangerNyTokenHentet).isEqualTo(1)
    }

    @Test
    fun verifiserAtTokenHentesPåNyttOmDetErUtgått() {
        var antallGangerNyTokenHentet = 0
        val tokenClient = AccessTokenProvider(config = config(), httpKlient = lagVerifiableHttpClient(-123) {antallGangerNyTokenHentet++})
        tokenClient.getBearerToken("scope")
        tokenClient.getBearerToken("scope")
        assertThat(antallGangerNyTokenHentet).isEqualTo(2)
    }

    private fun lagVerifiableHttpClient(utgårOmSekunder: Int, harHentetNyToken: () -> Unit) =
        HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(JavaTimeModule())
                }
            }
            engine {
                addHandler { request ->
                    harHentetNyToken()
                    respond(content =
                        """
                            {
                                "token_type": "enType",
                                "expires_in": $utgårOmSekunder,
                                "ext_expires_in": 123,
                                "access_token": "enTokenHeader.enTokenBody.enTokenSignatur"
                            }
                        """.trimIndent(),
                        headers = headersOf("Content-Type", "application/json")
                    )
                }
            }
        }

    private fun config() = AccessTokenProvider.Config(
        "secret",
        "clientId",
        "tokenEndpoint"
    )
}
