package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class KunFlywayMigreringTest {

    @Test
    fun `Bryteren er på bare når KUN_FLYWAY_MIGRERING er true`() {
        assertThat(skalKunKjøreFlywayMigrering(mapOf(KUN_FLYWAY_MIGRERING to "true"))).isTrue()
        assertThat(skalKunKjøreFlywayMigrering(mapOf(KUN_FLYWAY_MIGRERING to "TRUE"))).isTrue()
        assertThat(skalKunKjøreFlywayMigrering(mapOf(KUN_FLYWAY_MIGRERING to "false"))).isFalse()
        assertThat(skalKunKjøreFlywayMigrering(mapOf(KUN_FLYWAY_MIGRERING to ""))).isFalse()
        assertThat(skalKunKjøreFlywayMigrering(emptyMap())).isFalse()
    }

    @Test
    fun `Helseendepunktene svarer, men statistikk-endepunktet finnes ikke`() {
        val port = randomPort()
        val server = startKunHelseendepunkter(port = port, wait = false)
        val client = HttpClient(Apache5) { expectSuccess = false }

        try {
            runBlocking {
                assertThat(client.get("http://localhost:$port/isalive").status).isEqualTo(HttpStatusCode.OK)
                assertThat(client.get("http://localhost:$port/isready").status).isEqualTo(HttpStatusCode.OK)
                assertThat(client.get("${basePath(port)}/statistikk").status).isEqualTo(HttpStatusCode.NotFound)
            }
        } finally {
            client.close()
            server.stop(0, 0)
        }
    }
}
