package no.nav.statistikkapi.kandidatutfall

import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.stillinger.StillingRepository

class PresenterteKandidaterLytter(
    rapidsConnection: RapidsConnection,
    kandidatutfallRepository: KandidatutfallRepository,
    stillingRepository: StillingRepository
) :
    River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.rejectValue("@slutt_av_hendelseskjede", true)
                    it.demandValue("@event_name", "kandidat_v2.RegistrertDeltCv")
                    it.demandKey("stillingsinfo")
                    it.demandKey("stilling")
                    it.requireKey("stillingsinfo.stillingsid")
                    it.requireKey("aktørId")
                }
            }.register(this)
        }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId  = packet["kandidatlisteId"].asText()
        val tidspunkt  = packet["tidspunkt"].asLocalDate()
        val stillingsId  = packet["stillingsId"].asText()
        val utførtAvNavIdent  = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode  = packet["utførtAvNavKontorKode"].asText()
        val synligKandidat  = packet["synligKandidat"].asBoolean()
        val harHullICv  = packet["inkludering.harHullICv"].asBoolean()
        val alder  = packet["inkludering.alder"].asInt()
        val tilretteleggingsbehov  = packet["inkludering.tilretteleggingsbehov"].
        val harHullICv  = packet["inkludering.harHullICv"]
        val harHullICv  = packet["inkludering.harHullICv"]



        /*
    val harHullICv: Boolean,
    val alder: Int,
    val tilretteleggingsbehov: List<String>,
    val innsatsbehov: String,
    val hovedmål: String
    }


}
