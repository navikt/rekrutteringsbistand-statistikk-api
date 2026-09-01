package no.nav.statistikkapi.kandidatutfall

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.json.asTextNullable
import no.nav.statistikkapi.json.asUUIDNullable
import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.rapidsandrivers.requireValueIfPresent
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class ReverserPresenterteOgFåttJobbenKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
    private val utfallRepository: KandidatutfallRepository,
    private val eventNamePostfix: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) : River.PacketListener {
    private val secureLog = secure(log)

    init {
        River(rapidsConnection).apply {
            precondition { packet ->
                packet.requireValue("@event_name", "kandidat_v2.$eventNamePostfix")
                packet.requireKey("stillingsinfo")
                packet.requireValueIfPresent("@slutt_av_hendelseskjede", false)
            }
            validate {
                it.requireKey(
                    "tidspunkt",
                    "aktørId",
                    "utførtAvNavKontorKode",
                    "utførtAvNavIdent",
                    "kandidatlisteId",
                    "organisasjonsnummer",
                    "stillingsId"
                )

                it.interestedIn(
                    "@event_name",
                    "@slutt_av_hendelseskjede",
                    "stillingsinfo",
                    "stillingsinfo.stillingskategori",
                    "stillingsinfo.rekrutteringstreffId"
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
        val aktørId: String = packet["aktørId"].asString()
        val organisasjonsnummer: String = packet["organisasjonsnummer"].asString()
        val kandidatlisteId: String = packet["kandidatlisteId"].asString()
        val tidspunkt: ZonedDateTime = ZonedDateTime.parse(packet["tidspunkt"].asString())
        val stillingsId: String = packet["stillingsId"].asString()
        val stillingskategori: Stillingskategori =
            Stillingskategori.fraNavn(packet["stillingsinfo.stillingskategori"].asTextNullable())
        val rekrutteringstreffId = packet["stillingsinfo.rekrutteringstreffId"].asUUIDNullable()
        val utførtAvNavIdent: String = packet["utførtAvNavIdent"].asString()
        val utførtAvNavKontorKode: String = packet["utførtAvNavKontorKode"].asString()
        val utfall: Utfall =
            if (eventNamePostfix == "FjernetRegistreringDeltCv") Utfall.IKKE_PRESENTERT else Utfall.PRESENTERT

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
            secureLog.warn("Uventet utfall i databasen for event: $eventNamePostfix, utfallet er ${utfallFraDb.utfall}, aktørId: $aktørId, kandidatlisteId: $kandidatlisteId")
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
            hovedmål = utfallFraDb.hovedmål,
            rekrutteringstreffId = rekrutteringstreffId ?: utfallFraDb.rekrutteringstreffId,
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
    }
}
