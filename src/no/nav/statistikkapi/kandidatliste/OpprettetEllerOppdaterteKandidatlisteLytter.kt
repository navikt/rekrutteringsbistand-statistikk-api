package no.nav.statistikkapi.kandidatliste

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import java.time.ZonedDateTime

class OpprettetEllerOppdaterteKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatlisteRepository
): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandValue("@event_name", "kandidat_v2.OpprettetEllerOppdaterteKandidatliste")

                it.requireKey(
                    "stillingOpprettetTidspunkt",
                    "antallStillinger",
                    "erDirektemeldt",
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "stillingsId",
                    "utførtAvNavIdent",
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingOpprettetTidspunkt = packet["stillingOpprettetTidspunkt"].asZonedDateTime()
        val antallStillinger = packet["antallStillinger"].asInt()
        val erDirektemeldt = packet["erDirektemeldt"].asBoolean()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val stillingsId = packet["stillingsId"].asText()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()

        secureLog.info(
            """
                stillingOpprettetTidspunkt: $stillingOpprettetTidspunkt
                antallStillinger: $antallStillinger
                erDirektemeldt: $erDirektemeldt
                organisasjonsnummer: $organisasjonsnummer
                kandidatlisteId: $kandidatlisteId
                tidspunkt: $tidspunkt
                stillingsId: $stillingsId
                utførtAvNavIdent: $utførtAvNavIdent
            """.trimIndent()
        )

        val opprettKandidatliste = OpprettKandidatliste(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            antallStillinger = antallStillinger,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId,
            tidspunktForHendelsen = tidspunkt,
            stillingsId = stillingsId,
            navIdent = utførtAvNavIdent
        )

        if (repository.kandidatlisteFinnesIDB(opprettKandidatliste.kandidatlisteId)) {
            repository.oppdaterKandidatliste(opprettKandidatliste)
            log.info("Lagrer ikke fordi vi har lagret samme kandidatlisteutfall tidligere")
        } else {
            repository.lagreKandidatliste(opprettKandidatliste)
            log.info("Lagrer kandidatlistehendelse som kandidatlisteutfall")
        }

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }
}

data class OpprettKandidatliste(
    val stillingOpprettetTidspunkt: ZonedDateTime,
    val antallStillinger: Int,
    val erDirektemeldt: Boolean,
    val kandidatlisteId: String,
    val tidspunktForHendelsen: ZonedDateTime,
    val stillingsId: String,
    val navIdent: String
)