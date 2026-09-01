package no.nav.statistikkapi.kandidatutfall

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.json.asBooleanNullable
import no.nav.statistikkapi.json.asIntNullable
import no.nav.statistikkapi.json.asTextNullable
import no.nav.statistikkapi.json.asUUIDNullable
import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.rapidsandrivers.requireValueIfPresent
import no.nav.statistikkapi.stillinger.Stillingskategori
import tools.jackson.databind.JsonNode
import java.time.ZonedDateTime

class PresenterteOgFåttJobbenKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
    private val eventNamePostfix: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) : River.PacketListener {
    private val secureLog = secure(log)

    init {
        River(rapidsConnection).apply {
            precondition { packet ->
                packet.requireValue("@event_name", "kandidat_v2.$eventNamePostfix")
                packet.requireValueIfPresent("@slutt_av_hendelseskjede", false)
            }
            validate {
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
                    "@event_name",
                    "@slutt_av_hendelseskjede",
                    "stillingsinfo",
                    "stilling",
                    "stillingsId",
                    "stillingsinfo.stillingskategori",
                    "stillingsinfo.rekrutteringstreffId",
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

        val aktørId = packet["aktørId"].asString()
        val organisasjonsnummer = packet["organisasjonsnummer"].asString()
        val kandidatlisteId = packet["kandidatlisteId"].asString()
        val tidspunkt = ZonedDateTime.parse(packet["tidspunkt"].asString())
        val stillingsId = packet["stillingsId"].asTextNullable()
        val stillingskategori = packet["stillingsinfo.stillingskategori"].asTextNullable()
        val utfall = Utfall.fraEventNamePostfix(eventNamePostfix)
        val rekrutteringstreffId = packet["stillingsinfo.rekrutteringstreffId"].asUUIDNullable()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asString()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asString()
        val synligKandidat = packet["synligKandidat"].booleanValue()
        val harHullICv = packet["inkludering.harHullICv"].asBooleanNullable()
        val alder = packet["inkludering.alder"].asIntNullable()
        val innsatsbehov = packet["inkludering.innsatsbehov"].asTextNullable()
        val hovedmål = packet["inkludering.hovedmål"].asTextNullable()

        secureLog.info(
            """
            aktørId: $aktørId
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            stillingskategori: $stillingskategori
            rekrutteringstreffId: $rekrutteringstreffId
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
            hovedmål = hovedmål,
            rekrutteringstreffId = rekrutteringstreffId,
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
