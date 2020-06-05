package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.log

data class Kandidatutfall(
        val aktorId: String,
        val utfall: String,
        val navIdent: String,
        val navKontor: String,
        val kandidatlisteId: String,
        val stillingsId: String
)

fun Route.kandidatutfall(database: DatabaseInterface) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfall: Array<Kandidatutfall> = call.receive()
            log.info("Kandidatutfall: $kandidatutfall")

            kandidatutfall.forEach {
                database.lagreUtfall(it)
            }

            call.respond(HttpStatusCode.Created)
        }
    }
}
