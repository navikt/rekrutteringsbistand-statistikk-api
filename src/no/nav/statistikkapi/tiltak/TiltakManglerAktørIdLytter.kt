package no.nav.statistikkapi.tiltak

import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.logging.log

class TiltakManglerAktørIdLytter(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("tiltakstype")
                it.demandKey("avtaleInngått")
                it.rejectKey("aktørId")
                it.rejectKey("fnr")
                it.requireKey("deltakerFnr")
                it.requireKey("enhetOppfolging")
                it.requireKey("avtaleId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["fnr"] = packet["deltakerFnr"]
        packet["@event_name"] = "tiltakAvtaleInngått"
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Mangler obligatorisk felt $problems")
    }

}
