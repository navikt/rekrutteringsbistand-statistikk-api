package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode

import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.log

data class Kandidatutfall(
        val aktørId: String,
        val utfall: String,
        val navIdent: String,
        val navKontor: String,
        val kandidatlisteId: String,
        val stillingsId: String
)

fun Route.kandidatutfall(database: DatabaseInterface) {

    authenticate {
        post("/kandidatutfall") {
            val kandidatutfallListeString: String = call.receiveText()
            log.info("Kandidatutfall: \n${kandidatutfallListeString}")
            val mapper = jacksonObjectMapper()
            val kandidatutfallListe: List<Kandidatutfall> = mapper.readValue(kandidatutfallListeString)

            kandidatutfallListe.forEach {
                database.lagreUtfall(it)
            }

            call.respond(HttpStatusCode.Created)
        }
    }
}
