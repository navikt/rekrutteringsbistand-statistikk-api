package no.nav.statistikkapi.kandidatutfall

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
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
                    "organisasjonsnummer"
                )

                it.interestedIn(
                    "stillingsId"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val aktørId = packet["aktørId"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val stillingsId = packet["stillingsId"].asTextNullable()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asText()
        val utfall = Utfall.IKKE_PRESENTERT

        secureLog.info(
            """
            aktørId: $aktørId
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            utførtAvNavIdent: $utførtAvNavIdent
            utførtAvNavKontorKode: $utførtAvNavKontorKode
            utfall: $utfall
            """.trimIndent()
        )

        if (stillingsId == null) {
            log.info("Behandler ikke melding fordi den er uten stilingsId")
            return
        }
        val utfallFraDb = utfallRepository.hentSisteUtfallForKandidatIKandidatliste(aktørId, kandidatlisteId)

        if(utfallFraDb == null) {
            log.warn("Finner ikke utfallrad i databasen for event: $eventNamePostfix")
            return
        }
        if( !erForventetUtfall(eventNamePostfix, utfall)) {
            log.warn("Uventet utfall i databasen for event: $eventNamePostfix, utfallet er ${utfallFraDb.utfall}")
            return
        }

        val opprettKandidatutfall = OpprettKandidatutfall(
            aktørId = aktørId,
            utfall = utfall,
            navIdent = utførtAvNavIdent,
            navKontor = utførtAvNavKontorKode,
            kandidatlisteId = kandidatlisteId,
            stillingsId = utfallFraDb.stillingsId.toString(),
            synligKandidat = utfallFraDb.synligKandidat?:false,
            harHullICv = utfallFraDb.hullICv,
            alder = utfallFraDb.alder,
            tilretteleggingsbehov = utfallFraDb.tilretteleggingsbehov,
            tidspunktForHendelsen = tidspunkt,
            innsatsbehov = utfallFraDb.innsatsbehov,
            hovedmål = utfallFraDb.hovedmål
        )

        lagreUtfallOgStilling.lagreUtfall(
            kandidatutfall = opprettKandidatutfall
        )

        prometheusMeterRegistry.incrementUtfallLagret(opprettKandidatutfall.utfall)

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    private fun erForventetUtfall(eventNamePostfix: String, utfall: Utfall) = (eventNamePostfix == "FjernetRegistreringDeltCv" && utfall == Utfall.PRESENTERT ) ||
            (eventNamePostfix == "FjernetRegistreringFåttJobben" && utfall == Utfall.FATT_JOBBEN )

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}
