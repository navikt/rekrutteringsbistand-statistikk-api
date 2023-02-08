package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import no.nav.statistikkapi.stillinger.StillingRepository

class PresenterteOgFåttJobbenKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val kandidatRepository: KandidatutfallRepository,
    private val stillingRepository: StillingRepository,
    private val eventNamePostfix: String
) :
    River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.rejectValue("@slutt_av_hendelseskjede", true)
                    it.demandValue("@event_name", "kandidat_v2.$eventNamePostfix")
                    it.demandKey("stillingsinfo")
                    it.demandKey("stilling")
                    it.requireKey("stillingsinfo.stillingsid")
                    it.requireKey("aktørId")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("kandidatlisteId")
                    it.requireKey("tidspunkt")
                    it.interestedIn("stillingsId")
                    it.requireKey("utførtAvNavIdent")
                    it.requireKey("utførtAvNavKontorKode")
                    it.requireKey("synligKandidat")
                    it.interestedIn("inkludering.harHullICv")
                    it.interestedIn("inkludering.alder")
                    it.interestedIn("inkludering.tilretteleggingsbehov")
                    it.interestedIn("inkludering.innsatsbehov")
                    it.interestedIn("inkludering.hovedmål")
                }
            }.register(this)
        }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = packet["tidspunkt"].asLocalDate()
        val stillingsId = packet["stillingsId"].asText()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asText()
        val synligKandidat = packet["synligKandidat"].asBoolean()
        val harHullICv = packet["inkludering.harHullICv"].asBoolean()
        val alder = packet["inkludering.alder"].asInt()
        val tilretteleggingsbehov = packet["inkludering.tilretteleggingsbehov"].map(JsonNode::asText)
        val innsatsbehov = packet["inkludering.innsatsbehov"].asText()
        val hovedmål = packet["inkludering.hovedmål"].asText()
        val utfall = Utfall.fraEventNamePostfix(eventNamePostfix)

        secureLog.info("""
            aktørId: $aktørId
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            utførtAvNavIdent: $utførtAvNavIdent
            utførtAvNavKontorKode: $utførtAvNavKontorKode
            synligKandidat: $synligKandidat
            harHullICv: $harHullICv
            alder: $alder
            tilretteleggingsbehov: $tilretteleggingsbehov
            innsatsbehov: $innsatsbehov
            hovedmål: $hovedmål
            utfall: $utfall
            """.trimIndent())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}
