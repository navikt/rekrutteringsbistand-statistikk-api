package no.nav.statistikkapi.kandidatliste

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.statistikkapi.json.asZonedDateTime
import no.nav.statistikkapi.json.asZonedDateTimeNullable
import no.nav.statistikkapi.logging.log
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
            precondition { packet ->
                packet.requireAny(
                    "@event_name",
                    listOf(opprettetKandidatlisteEventName, oppdaterteKandidatlisteEventName)
                )
                packet.requireKey("stilling", "stilling.stillingensPubliseringstidspunkt")
                packet.forbidValue("@slutt_av_hendelseskjede", true)
            }
            validate {
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

                it.interestedIn(
                    "@event_name",
                    "stilling.stillingOpprettetTidspunkt"
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
        val stillingOpprettetTidspunkt = packet["stilling.stillingOpprettetTidspunkt"].asZonedDateTimeNullable()
        val stillingensPubliseringstidspunkt = packet["stilling.stillingensPubliseringstidspunkt"].asZonedDateTime()
        val antallStillinger = packet["stilling.antallStillinger"].intValue()
        val erDirektemeldt = packet["stilling.erDirektemeldt"].booleanValue()
        val antallKandidater = packet["antallKandidater"].intValue()
        val organisasjonsnummer = packet["organisasjonsnummer"].asString()
        val kandidatlisteId = packet["kandidatlisteId"].asString()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val stillingsId = packet["stillingsId"].asString()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asString()
        val eventName = packet["@event_name"].asString()

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

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}

data class Kandidatlistehendelse(
    val stillingOpprettetTidspunkt: ZonedDateTime?,
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
