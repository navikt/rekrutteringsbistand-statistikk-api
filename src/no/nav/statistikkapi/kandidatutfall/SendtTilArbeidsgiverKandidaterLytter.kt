package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.LocalDate
import java.time.ZonedDateTime

class SendtTilArbeidsgiverKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandValue("@event_name", "kandidat_v2.DelCvMedArbeidsgiver")
                it.requireKey(
                    "stillingsId",
                    "stillingstittel",
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                    "utførtAvNavKontorKode",
                    "arbeidsgiversEpostadresser",
                    "meldingTilArbeidsgiver",
                    "kandidater"
                )
                it.interestedIn("stillingsinfo.stillingskategori")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId = packet["stillingsId"].asText()
        val stillingstittel = packet["stillingstittel"].asText()
        val stillingskategori = packet["stillingsinfo.stillingskategori"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asText()
        val arbeidsgiversEpostadresser = packet["arbeidsgiversEpostadresser"].map(JsonNode::asText)
        val meldingTilArbeidsgiver = packet["meldingTilArbeidsgiver"].asText()

        packet["kandidater"].fields().forEach { (aktørId, node) ->
            val harHullICv = node["harHullICv"].asBoolean()
            val alder = node["alder"].asInt()
            val tilretteleggingsbehov = node["tilretteleggingsbehov"].map(JsonNode::asText)
            val innsatsbehov = node["innsatsbehov"].asText()
            val hovedmål = node["hovedmål"].asText()

            secureLog.info(
                """
            stillingsId: $stillingsId
            stillingstittel: $stillingstittel
            stillingskategori: $stillingskategori
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            utførtAvNavIdent: $utførtAvNavIdent
            utførtAvNavKontorKode: $utførtAvNavKontorKode
            arbeidsgiversEpostadresser: $arbeidsgiversEpostadresser
            meldingTilArbeidsgiver: $meldingTilArbeidsgiver
            aktørId: $aktørId
            harHullICv: $harHullICv
            alder: $alder
            tilretteleggingsbehov: $tilretteleggingsbehov
            innsatsbehov: $innsatsbehov
            hovedmål: $hovedmål
            """.trimIndent()
            )

            val opprettKandidatutfall = OpprettKandidatutfall(
                aktørId = aktørId,
                utfall = Utfall.PRESENTERT,
                navIdent = utførtAvNavIdent,
                navKontor = utførtAvNavKontorKode,
                kandidatlisteId = kandidatlisteId,
                stillingsId = stillingsId,
                synligKandidat = true,
                harHullICv = harHullICv,
                alder = alder,
                tilretteleggingsbehov = tilretteleggingsbehov,
                tidspunktForHendelsen = tidspunkt
            )

            lagreUtfallOgStilling.lagreUtfallOgStilling(
                kandidatutfall = opprettKandidatutfall,
                stillingsid = stillingsId,
                stillingskategori = Stillingskategori.fraNavn(stillingskategori)
            )

            packet["@slutt_av_hendelseskjede"] = true
            context.publish(packet.toJson())
        }

    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}

private fun JsonNode.asZonedDateTime() =
        asText().let ( ZonedDateTime::parse )