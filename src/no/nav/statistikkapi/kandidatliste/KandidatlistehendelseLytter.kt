package no.nav.statistikkapi.kandidatliste

import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
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

                it.demandKey("stilling")
                it.demandKey("stilling.stillingensPubliseringstidspunkt")
                it.demandKey("stilling.stillingOpprettetTidspunkt")

                it.requireKey(
                    "stilling.antallStillinger",
                    "stilling.erDirektemeldt",
                    "antallKandidater",
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
        val stillingOpprettetTidspunkt = packet["stilling.stillingOpprettetTidspunkt"].asZonedDateTime()
        val stillingensPubliseringstidspunkt = packet["stilling.stillingensPubliseringstidspunkt"].asZonedDateTime()
        val antallStillinger = packet["stilling.antallStillinger"].asInt()
        val erDirektemeldt = packet["stilling.erDirektemeldt"].asBoolean()
        val antallKandidater = packet["antallKandidater"].asInt()
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
        val erDuplikat = repository.hendelseFinnesFraFør(hendelse.eventName, listeId, hendelse.tidspunkt)
        val harMottattOpprettetMelding = repository.harMottattOpprettetMelding(listeId)

        if (eventName == opprettetKandidatlisteEventName && harMottattOpprettetMelding) {
            log.warn("Ignorerer melding. Fikk opprettmelding for en kandidatliste som er opprettet fra før. eventName=${hendelse.eventName}, kandidatlisteId=${hendelse.kandidatlisteId}")
            return
        }
        if (erDuplikat) {
            log.info("Har behandlet meldingen tidligere. Ignorerer den.")
            return
        }

        repository.lagreKandidatlistehendelse(hendelse)

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

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
