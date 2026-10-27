package no.nav.statistikkapi

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Bryter for migreringen fra FSS til GCP: Når den er på, kjører appen bare Flyway-migreringene og svarer på helsesjekker.
 * Appen leser ikke fra rapiden, sender ikke til Datavarehus og har ikke statistikk-endepunktet.
 */
const val KUN_FLYWAY_MIGRERING = "KUN_FLYWAY_MIGRERING"

fun skalKunKjøreFlywayMigrering(env: Map<String, String> = System.getenv()): Boolean =
    env[KUN_FLYWAY_MIGRERING].equals("true", ignoreCase = true)

fun startKunHelseendepunkter(port: Int, wait: Boolean = true): EmbeddedServer<*, *> =
    embeddedServer(Netty, port = port) {
        routing {
            get("/isalive") { call.respondText("ALIVE") }
            get("/isready") { call.respondText("READY") }
        }
    }.start(wait = wait)
