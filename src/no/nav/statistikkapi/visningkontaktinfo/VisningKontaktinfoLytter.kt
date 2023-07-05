package no.nav.statistikkapi.visningkontaktinfo

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.kandidatutfall.asUUID
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
import no.nav.statistikkapi.logWithoutClassname

class VisningKontaktinfoLytter(
    rapidsConnection: RapidsConnection,
    private val repository: VisningKontaktinfoRepository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandValue("@event_name", "arbeidsgiversKandidatliste.VisningKontaktinfo")
                it.requireKey( "aktørId", "stillingsId", "tidspunkt")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val stillingsId = packet["stillingsId"].asUUID()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()

        val alleredeLagret = repository.harAlleredeBlittLagret(aktørId, stillingsId, tidspunkt)

        if (alleredeLagret) {
            logWithoutClassname.info("Melding om visning av kontaktinfo har allerede blitt lagret, så ignorerer melding")
        } else {
            repository.lagre(aktørId, stillingsId, tidspunkt)
        }

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }
}
