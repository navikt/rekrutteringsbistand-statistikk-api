package statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import org.junit.Test
import statistikkapi.stillinger.autentisering.StillingssokProxyAccessTokenKlient

class StillingssokProxyAccessTokenKlientTest {

    @Test
    fun verifiserAtNyTokenHentesVedOpprettelseAvKlient() {
        var antallGangerNyTokenHentet = 0
        StillingssokProxyAccessTokenKlient(httpKlient = lagVerifiableHttpClient(123) {antallGangerNyTokenHentet++}, config = config())
        assertThat(antallGangerNyTokenHentet).isEqualTo(1)
    }

    @Test
    fun verifiserAtTokenKunHentesEnGang() {
        var antallGangerNyTokenHentet = 0
        val tokenClient = StillingssokProxyAccessTokenKlient(httpKlient = lagVerifiableHttpClient(123) {antallGangerNyTokenHentet++}, config = config())
        tokenClient.getBearerToken()
        assertThat(antallGangerNyTokenHentet).isEqualTo(1)
    }

    @Test
    fun verifiserAtTokenHentesPåNyttOmDetErUtgått() {
        var antallGangerNyTokenHentet = 0
        val tokenClient = StillingssokProxyAccessTokenKlient(httpKlient = lagVerifiableHttpClient(-123) {antallGangerNyTokenHentet++}, config = config())
        tokenClient.getBearerToken()
        assertThat(antallGangerNyTokenHentet).isEqualTo(2)
    }

    private fun lagVerifiableHttpClient(utgårOmSekunder: Int, harHentetNyToken: () -> Unit) =
        HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
                                "access_token": "enToken"
                            }
                        """.trimIndent(),
                        headers = headersOf("Content-Type", "application/json")
                    )
                }
            }
        }

    private fun config() = StillingssokProxyAccessTokenKlient.AuthenticationConfig(
        "secret",
        "clientId",
        "tenantId"
    )
}
