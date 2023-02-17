package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import no.nav.statistikkapi.stillinger.Stillingskategori
import no.nav.statistikkapi.toOslo
import java.time.ZonedDateTime

class SlettetStillingOgKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatutfallRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.requireKey("stillingsinfo")
                it.demandValue("@event_name", "kandidat_v2.SlettetStillingOgKandidatliste")

                it.requireKey(
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()

        secureLog.info(
            """
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            utførtAvNavIdent: $utførtAvNavIdent
            """.trimIndent()
        )

        val sisteUtfallForAlleKandidater: List<Kandidatutfall> = repository.hentSisteUtfallTilAlleKandidater(kandidatlisteId)
        sisteUtfallForAlleKandidater.forEach {
            val nyttUtfall = it.copy(utfall = Utfall.IKKE_PRESENTERT, tidspunkt = tidspunkt.toLocalDateTime(), navIdent = utførtAvNavIdent, navKontor = "")
            repository.lagreUtfall(nyttUtfall) // TODO: Opprett, ikke bare kopier det som er hentet fra db
        }

        // TODO: Gjøre i foreach
        //prometheusMeterRegistry.incrementUtfallLagret(opprettKandidatutfall.utfall)

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }


    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }

}
