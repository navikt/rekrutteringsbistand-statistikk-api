package no.nav.statistikkapi.kandidatutfall

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.logging.SecureLog
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class SlettetStillingOgKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val repository: KandidatutfallRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling
) : River.PacketListener {
    private val secureLog = SecureLog(log)
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kandidat_v2.SlettetStillingOgKandidatliste")
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.demandKey("stillingsinfo")
                it.interestedIn("stillingsinfo.stillingskategori")

                it.requireKey(
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                    "stillingsId"
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
        val kandidatlisteId: String = packet["kandidatlisteId"].asText()
        val tidspunkt: ZonedDateTime = ZonedDateTime.parse(packet["tidspunkt"].asText())
        val utførtAvNavIdent: String = packet["utførtAvNavIdent"].asText()
        val stillingsId: String = packet["stillingsId"].asText()
        val stillingskategori: Stillingskategori =
            Stillingskategori.fraNavn(packet["stillingsinfo.stillingskategori"].asTextNullable())

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
            if (it.utfall == Utfall.PRESENTERT || it.utfall == Utfall.FATT_JOBBEN) {
                val nyttUtfall = OpprettKandidatutfall(
                    utfall = Utfall.IKKE_PRESENTERT,
                    aktørId = it.aktorId,
                    navIdent = utførtAvNavIdent,
                    navKontor = "",
                    kandidatlisteId = it.kandidatlisteId.toString(),
                    stillingsId = it.stillingsId.toString(),
                    synligKandidat = it.synligKandidat ?: false,
                    harHullICv = null,
                    innsatsbehov = null,
                    hovedmål = null,
                    alder = null,
                    tidspunktForHendelsen = tidspunkt
                )
                lagreUtfallOgStilling.lagreUtfallOgStilling(nyttUtfall, stillingsId, stillingskategori)
            }
        }

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}
