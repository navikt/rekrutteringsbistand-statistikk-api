package no.nav.statistikkapi.tiltak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
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

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        packet["fnr"] = packet["deltakerFnr"]
        packet["@event_name"] = "tiltakAvtaleInngått"
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Mangler obligatorisk felt $problems")
    }
}
