package no.nav.statistikkapi.tiltak

import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingRepository

class Tiltaklytter(
    rapidsConnection: RapidsConnection,
    private val repo: StillingRepository,
) : PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("tiltakstype")
                it.demandKey("avtaleInngått")
                it.requireKey("deltakerFnr")
                it.requireKey("opprettetTidspunkt")
                it.requireKey("enhetOppfolging")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val tiltakstype = packet["tiltakstype"].asText()
        val deltakerFnr = packet["deltakerFnr"].asText()
        val opprettetTidspunkt = packet["opprettetTidspunkt"].asText()
        val enhetOppfolging = packet["enhetOppfolging"].asText()

        log.info("Tiltaksmelding mottatt tiltakstype:$tiltakstype, " +
                "deltakerFnr: $deltakerFnr, opprettetTidspunkt: $opprettetTidspunkt" +
                " enhetOppfolging: $enhetOppfolging")
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Mangler oblligatorisk felt ${problems.toExtendedReport()}")
    }


}

/*
    id SERIAL PRIMARY KEY,
    aktorid TEXT,
    fnr TEXT,
    navkontor TEXT,
    tidspunkt timestamp
 */