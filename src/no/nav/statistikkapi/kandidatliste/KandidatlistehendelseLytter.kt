package no.nav.statistikkapi.kandidatliste

import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
import no.nav.statistikkapi.log
import java.time.ZonedDateTime

const val OpprettetKandidatlisteHendelse = "kandidat_v2.OpprettetKandidatliste"
const val OppdaterteKandidatlisteHendelse = "kandidat_v2.OppdaterteKandidatliste"

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
                    listOf(OpprettetKandidatlisteHendelse, OppdaterteKandidatlisteHendelse)
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
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
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

        val kandidatlistehendelse = OpprettKandidatliste(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            stillingensPubliseringstidspunkt = stillingensPubliseringstidspunkt,
            antallStillinger = antallStillinger,
            antallKandidater = antallKandidater,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId,
            tidspunkt = tidspunkt,
            stillingsId = stillingsId,
            organisasjonsnummer = organisasjonsnummer,
            utførtAvNavIdent = utførtAvNavIdent
        )

        val erEndremeldingPåIkkeeksisterendeListe =
            eventName != OpprettetKandidatlisteHendelse &&
                    !repository.kandidatlisteFinnesIDB(kandidatlistehendelse.kandidatlisteId)



        if (erEndremeldingPåIkkeeksisterendeListe) {
            log.warn("Fikk endremelding ($eventName) på listen $kandidatlisteId, men denne forkastes fordi listen ikke finnes i databasen")
            return
        }

        repository.lagreKandidatlistehendelse(eventName, kandidatlistehendelse)

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
)


typealias OpprettKandidatliste = Kandidatlistehendelse
typealias OppdaterKandidatliste = OpprettKandidatliste