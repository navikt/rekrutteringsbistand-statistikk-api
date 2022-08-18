package no.nav.statistikkapi.kandidatutfall

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.log
import java.time.ZonedDateTime

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

    data class Kandidathendelse(
        val type: Type,
        val aktørId: String,
        val organisasjonsnummer: String,
        val kandidatlisteId: String,
        val tidspunkt: ZonedDateTime,
        val stillingsId: String,
        val utførtAvNavIdent: String,
        val utførtAvNavKontorKode: String,
        val synligKandidat: Boolean,
        val harHullICv: Boolean,
        val alder: Int,
        val tilretteleggingsbehov: List<String>,
    )

    enum class Type(private val eventNamePostfix: String) {
        CV_DELT_UTENFOR_REKRUTTERINGSBISTAND("cv-delt-med-arbeidsgiver-utenfor-rekrutteringsbistand"),
        CV_DELT_VIA_REKRUTTERINGSBISTAND("cv-delt-med-arbeidsgiver-via-rekrutteringsbistand");
    }
}
