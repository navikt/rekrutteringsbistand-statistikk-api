package no.nav.statistikkapi.kandidatutfall

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.log

class Kandidathendelselytter(rapidsConnection: RapidsConnection): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kandidat.cv-delt-med-arbeidsgiver-via-rekrutteringsbistand")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("Mottok en melding om at CV er delt med arbeidsgiver via rekrutteringsbistand men gjør ingenting med den!")
        // Vi kan ha gamle eventer som har utc tidssone. Kjør dette for sikkerhetsskyld på tidspunktene:
        // it.copy(tidspunktForHendelsen = it.tidspunktForHendelsen.withZoneSameInstant(ZoneId.of("Europe/Oslo")))
    }
}