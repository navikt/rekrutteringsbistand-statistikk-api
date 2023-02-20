package no.nav.statistikkapi.kandidatutfall

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import java.time.ZonedDateTime

class SlettetStillingOgKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatutfallRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.rejectKey("stillingsinfo")
                it.demandValue("@event_name", "kandidat_v2.SlettetStillingOgKandidatliste")

                it.requireKey(
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()

        secureLog.info(
            """
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            utførtAvNavIdent: $utførtAvNavIdent
            """.trimIndent()
        )

        if(kandidatlisteId==null) {
            log.info("lagrer ikke utfall pga at kandidatlisteid er null")
            return
        }

        val kandidatutfall: List<Kandidatutfall> = repository.hentAlleUtfallTilhørendeKandidatliste(kandidatlisteId)
            .groupBy(Kandidatutfall::aktorId)
            .map { it.value.maxByOrNull(Kandidatutfall::tidspunkt)!! }

        kandidatutfall.forEach {
            if(it.utfall == Utfall.PRESENTERT || it.utfall == Utfall.FATT_JOBBEN) {
                val nyttUtfall =  OpprettKandidatutfall(
                    utfall = Utfall.IKKE_PRESENTERT,
                    aktørId = it.aktorId,
                    navIdent = utførtAvNavIdent,
                    navKontor = "",
                    kandidatlisteId = it.kandidatlisteId.toString(),
                    stillingsId = it.stillingsId?.toString(),
                    synligKandidat = it.synligKandidat?:false,
                    harHullICv = null,
                    innsatsbehov = null,
                    hovedmål = null,
                    alder = null,
                    tilretteleggingsbehov = emptyList(),
                    tidspunktForHendelsen = tidspunkt
                )
                repository.lagreUtfall(nyttUtfall)
                prometheusMeterRegistry.incrementUtfallLagret(Utfall.IKKE_PRESENTERT)
            }
        }

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}
