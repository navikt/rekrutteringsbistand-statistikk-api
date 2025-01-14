package no.nav.statistikkapi.kandidatutfall

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
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

class ReverserPresenterteOgFåttJobbenKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
    private val utfallRepository: KandidatutfallRepository,
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
                    "utførtAvNavKontorKode",
                    "utførtAvNavIdent",
                    "kandidatlisteId",
                    "organisasjonsnummer",
                    "stillingsId"
                )

                it.demandKey("stillingsinfo")
                it.interestedIn("stillingsinfo.stillingskategori")

            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId: String = packet["aktørId"].asText()
        val organisasjonsnummer: String = packet["organisasjonsnummer"].asText()
        val kandidatlisteId: String = packet["kandidatlisteId"].asText()
        val tidspunkt: ZonedDateTime = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val stillingsId: String = packet["stillingsId"].asText()
        val stillingskategori: Stillingskategori =
            Stillingskategori.fraNavn(packet["stillingsinfo.stillingskategori"].asTextNullable())
        val utførtAvNavIdent: String = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode: String = packet["utførtAvNavKontorKode"].asText()
        val utfall: Utfall =
            if (eventNamePostfix == "FjernetRegistreringDeltCv") Utfall.IKKE_PRESENTERT else Utfall.PRESENTERT

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
            utfall: $utfall
            """.trimIndent()
        )

        val utfallFraDb = utfallRepository.hentSisteUtfallForKandidatIKandidatliste(aktørId, kandidatlisteId)

        if (utfallFraDb == null) {
            log.warn("Finner ikke utfallrad i databasen for event: $eventNamePostfix")
            return
        }
        if (!erForventetUtfall(eventNamePostfix, utfallFraDb.utfall)) {
            log.warn("Uventet utfall i databasen for event: $eventNamePostfix, utfallet er ${utfallFraDb.utfall}, sjekk secureLog for mer informasjon")
            secure(log).warn("Uventet utfall i databasen for event: $eventNamePostfix, utfallet er ${utfallFraDb.utfall}, aktørId: $aktørId, kandidatlisteId: $kandidatlisteId")
            return
        }

        val opprettKandidatutfall = OpprettKandidatutfall(
            aktørId = aktørId,
            utfall = utfall,
            navIdent = utførtAvNavIdent,
            navKontor = utførtAvNavKontorKode,
            kandidatlisteId = kandidatlisteId,
            stillingsId = utfallFraDb.stillingsId.toString(),
            synligKandidat = utfallFraDb.synligKandidat ?: false,
            harHullICv = utfallFraDb.hullICv,
            alder = utfallFraDb.alder,
            tidspunktForHendelsen = tidspunkt,
            innsatsbehov = utfallFraDb.innsatsbehov,
            hovedmål = utfallFraDb.hovedmål
        )


        lagreUtfallOgStilling.lagreUtfallOgStilling(
            kandidatutfall = opprettKandidatutfall,
            stillingsId,
            stillingskategori
        )

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    private fun erForventetUtfall(eventNamePostfix: String, utfall: Utfall) =
        (eventNamePostfix == "FjernetRegistreringDeltCv" && utfall == Utfall.PRESENTERT) ||
                (eventNamePostfix == "FjernetRegistreringFåttJobben" && utfall == Utfall.FATT_JOBBEN)

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av melding\n$problems")
        super.onError(problems, context, metadata)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}
