package no.nav.statistikkapi.tiltak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.logging.log
import java.time.ZonedDateTime
import java.util.*

class Tiltaklytter(
    rapidsConnection: RapidsConnection,
    private val repo: TiltaksRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("tiltakstype")
                it.demandKey("avtaleInngått")
                it.demandKey("aktørId")
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.requireKey("deltakerFnr")
                it.requireKey("enhetOppfolging")
                it.requireKey("avtaleId")
                it.requireKey("sistEndret")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val avtaleId = UUID.fromString(packet["avtaleId"].asText())
        val deltakerAktørId = packet["aktørId"].asText()
        val deltakerFnr = packet["deltakerFnr"].asText()
        val enhetOppfolging = packet["enhetOppfolging"].asText()
        val tiltakstype = packet["tiltakstype"].asText()
        val avtaleInngått = packet["avtaleInngått"].asLocalDateTime().atOslo()
        val sistEndret = ZonedDateTime.parse(packet["sistEndret"].asText()).atOslo()


        log.info("Tiltaksmelding mottatt tiltakstype: avtaleId: ${avtaleId}")


        val tiltak = TiltaksRepository.OpprettTiltak(
            avtaleId = avtaleId,
            deltakerAktørId = deltakerAktørId,
            deltakerFnr = deltakerFnr,
            enhetOppfolging = enhetOppfolging,
            tiltakstype = tiltakstype,
            avtaleInngått = avtaleInngått,
            sistEndret = sistEndret
        )
        repo.lagreTiltak(tiltak)
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Mangler oblligatorisk felt $problems")
        super.onError(problems, context, metadata)
    }
}
