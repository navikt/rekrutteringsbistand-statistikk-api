package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class PresenterteOgFåttJobbenKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
    private val eventNamePostfix: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)

                it.demandValue("@event_name", "kandidat_v2.$eventNamePostfix")

                it.requireKey(
                    "tidspunkt",
                    "aktørId",
                    "synligKandidat",
                    "utførtAvNavKontorKode",
                    "utførtAvNavIdent",
                    "kandidatlisteId",
                    "organisasjonsnummer"
                )

                it.interestedIn(
                    "stillingsinfo",
                    "stilling",
                    "stillingsId",
                    "stillingsinfo.stillingskategori",
                    "inkludering.harHullICv",
                    "inkludering.alder",
                    "inkludering.innsatsbehov",
                    "inkludering.hovedmål"
                )
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        if (!erEntenKomplettStillingEllerIngenStilling(packet)) return

        val aktørId = packet["aktørId"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val stillingsId = packet["stillingsId"].asTextNullable()
        val stillingskategori = packet["stillingsinfo.stillingskategori"].asTextNullable()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asText()
        val synligKandidat = packet["synligKandidat"].asBoolean()
        val harHullICv = packet["inkludering.harHullICv"].asBooleanNullable()
        val alder = packet["inkludering.alder"].asIntNullable()
        val innsatsbehov = packet["inkludering.innsatsbehov"].asTextNullable()
        val hovedmål = packet["inkludering.hovedmål"].asTextNullable()
        val utfall = Utfall.fraEventNamePostfix(eventNamePostfix)

        secure(log).info(
            """
            aktørId: $aktørId
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            stillingskategori: $stillingskategori
            utførtAvNavIdent: $utførtAvNavIdent
            utførtAvNavKontorKode: $utførtAvNavKontorKode
            synligKandidat: $synligKandidat
            harHullICv: $harHullICv
            alder: $alder
            innsatsbehov: $innsatsbehov
            hovedmål: $hovedmål
            utfall: $utfall
            """.trimIndent()
        )

        if (stillingsId == null) {
            log.info("Behandler ikke melding fordi den er uten stilingsId")
            return
        }

        val opprettKandidatutfall = OpprettKandidatutfall(
            aktørId = aktørId,
            utfall = utfall,
            navIdent = utførtAvNavIdent,
            navKontor = utførtAvNavKontorKode,
            kandidatlisteId = kandidatlisteId,
            stillingsId = stillingsId,
            synligKandidat = synligKandidat,
            harHullICv = harHullICv,
            alder = alder,
            tidspunktForHendelsen = tidspunkt,
            innsatsbehov = innsatsbehov,
            hovedmål = hovedmål
        )

        lagreUtfallOgStilling.lagreUtfallOgStilling(
            kandidatutfall = opprettKandidatutfall,
            stillingsid = stillingsId,
            stillingskategori = Stillingskategori.fraNavn(stillingskategori)
        )

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    private fun erEntenKomplettStillingEllerIngenStilling(packet: JsonMessage): Boolean =
        packet["stillingsId"].isMissingOrNull() ||
                (packet["stilling"].exists() && packet["stillingsinfo"].exists())

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}

fun JsonNode.exists() = !isMissingOrNull()
