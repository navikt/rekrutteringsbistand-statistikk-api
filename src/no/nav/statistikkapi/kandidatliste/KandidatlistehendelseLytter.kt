package no.nav.statistikkapi.kandidatliste

import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
import no.nav.statistikkapi.kandidatutfall.exists
import no.nav.statistikkapi.log
import java.time.ZonedDateTime
import java.util.*

const val opprettetKandidatlisteEventName = "kandidat_v2.OpprettetKandidatliste"
const val oppdaterteKandidatlisteEventName = "kandidat_v2.OppdaterteKandidatliste"

class KandidatlistehendelseLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatlisteRepository
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandAny(
                    "@event_name",
                    listOf(opprettetKandidatlisteEventName, oppdaterteKandidatlisteEventName)
                )

                it.requireKey(
                    "stillingOpprettetTidspunkt",
                    "stillingensPubliseringstidspunkt",
                    "antallStillinger",
                    "antallKandidater",
                    "erDirektemeldt",
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "stillingsId",
                    "utførtAvNavIdent",
                )
                it.interestedIn("stilling", "stillingsinfo")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (!erEntenKomplettStillingEllerIngenStilling(packet)) return

        val stillingOpprettetTidspunkt = packet["stillingOpprettetTidspunkt"].asZonedDateTime()
        val stillingensPubliseringstidspunkt = packet["stillingensPubliseringstidspunkt"].asZonedDateTime()
        val antallStillinger = packet["antallStillinger"].asInt()
        val antallKandidater = packet["antallKandidater"].asInt()
        val erDirektemeldt = packet["erDirektemeldt"].asBoolean()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val stillingsId = packet["stillingsId"].asText()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val eventName = packet["@event_name"].asText()

        val hendelse = Kandidatlistehendelse(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            stillingensPubliseringstidspunkt = stillingensPubliseringstidspunkt,
            antallStillinger = antallStillinger,
            antallKandidater = antallKandidater,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId,
            tidspunkt = tidspunkt,
            stillingsId = stillingsId,
            organisasjonsnummer = organisasjonsnummer,
            utførtAvNavIdent = utførtAvNavIdent,
            eventName = eventName
        )

        val listeId = UUID.fromString(hendelse.kandidatlisteId)
        val finnesFraFør = repository.hendelseFinnesFraFør(hendelse.eventName, listeId, hendelse.tidspunkt)
        val oppdaterhendelseManglerOppretthendelse =
            hendelse.eventName == oppdaterteKandidatlisteEventName && !repository.kandidatlisteFinnesIDb(listeId)

        if (finnesFraFør) {
            log.warn("Ignorerer melding. Fikk opprettmelding for en kandidatliste som er opprettet ffra før. eventName=${hendelse.eventName}, kandidatlisteId=${hendelse.kandidatlisteId}")
            return
        } else if (oppdaterhendelseManglerOppretthendelse) {
            log.warn("Ignorerer melding. Fikk oppdatermelding for en kandidatliste som ikke har blitt opprettet. eventName=${hendelse.eventName}, kandidatlisteId=${hendelse.kandidatlisteId}")
            return
        }

        repository.lagreKandidatlistehendelse(hendelse)

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    private fun erEntenKomplettStillingEllerIngenStilling(packet: JsonMessage): Boolean =
        packet["stillingsId"].isMissingOrNull() ||
                (packet["stilling"].exists() && packet["stillingsinfo"].exists())

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}

data class Kandidatlistehendelse(
    val stillingOpprettetTidspunkt: ZonedDateTime,
    val stillingensPubliseringstidspunkt: ZonedDateTime,
    val organisasjonsnummer: String,
    val antallStillinger: Int,
    val antallKandidater: Int,
    val erDirektemeldt: Boolean,
    val kandidatlisteId: String,
    val tidspunkt: ZonedDateTime,
    val stillingsId: String,
    val utførtAvNavIdent: String,
    val eventName: String
)
