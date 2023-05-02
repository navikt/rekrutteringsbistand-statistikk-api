package no.nav.statistikkapi.kandidatutfall

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.secureLog
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class SlettetStillingOgKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatutfallRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kandidat_v2.SlettetStillingOgKandidatliste")
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandKey("stillingsinfo")
                it.interestedIn("stillingsinfo.stillingskategori")

                it.requireKey(
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                    "stillingsId"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidatlisteId: String = packet["kandidatlisteId"].asText()
        val tidspunkt: ZonedDateTime = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val utførtAvNavIdent: String = packet["utførtAvNavIdent"].asText()
        val stillingsId: String = packet["stillingsId"].asText()
        val stillingskategori: Stillingskategori =  Stillingskategori.fraNavn(packet["stillingsinfo.stillingskategori"].asTextNullable())

        secureLog.info(
            """
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            utførtAvNavIdent: $utførtAvNavIdent
            """.trimIndent()
        )

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
                    stillingsId = it.stillingsId.toString(),
                    synligKandidat = it.synligKandidat?:false,
                    harHullICv = null,
                    innsatsbehov = null,
                    hovedmål = null,
                    alder = null,
                    tidspunktForHendelsen = tidspunkt
                )
                lagreUtfallOgStilling.lagreUtfallOgStilling(nyttUtfall, stillingsId, stillingskategori)
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
