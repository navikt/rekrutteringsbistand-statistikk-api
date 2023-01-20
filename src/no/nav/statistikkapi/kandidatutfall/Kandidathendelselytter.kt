package no.nav.statistikkapi.kandidatutfall

import io.micrometer.core.instrument.Metrics
import no.nav.helse.rapids_rivers.*
import no.nav.statistikkapi.log
import no.nav.statistikkapi.objectMapper
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import no.nav.statistikkapi.toOslo
import java.time.ZonedDateTime

class Kandidathendelselytter(
    rapidsConnection: RapidsConnection,
    private val repo: KandidatutfallRepository,
    private val stillingRepository: StillingRepository
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny(
                    key = "@event_name",
                    values = Type.values().map { "kandidat.${it.eventName}" }
                )
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.requireKey("kandidathendelse")
                it.demandKey("stillingsinfo")
                it.requireKey("stillingsinfo.stillingsid")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidathendelse: Kandidathendelse =
            objectMapper.treeToValue(packet["kandidathendelse"], Kandidathendelse::class.java)

        log.info("Har mottatt kandidathendelse")

        if (kandidathendelse.stillingsId == null) {
            log.info("Behandler ikke melding fordi den er uten stilingsId")
            return
        }

        val opprettKandidatutfall: OpprettKandidatutfall = kandidathendelse.toOpprettKandidatutfall()

        val stillingsinfo = objectMapper.treeToValue(packet["stillingsinfo"], StillingsinfoIHendelse::class.java)
        stillingRepository.lagreStilling(stillingsinfo.stillingsid, stillingsinfo.stillingskategori)

        if (repo.kandidatutfallAlleredeLagret(opprettKandidatutfall)) {
            log.info("Lagrer ikke fordi vi har lagret samme utfall tidligere")
        } else if (repo.hentSisteUtfallForKandidatIKandidatliste(opprettKandidatutfall) == opprettKandidatutfall.utfall) {
            log.info("Lagrer ikke fordi siste kandidatutfall for samme kandidat og kandidatliste har likt utfall")
        } else {
            repo.lagreUtfall(opprettKandidatutfall)
            log.info("Lagrer kandidathendelse som kandidatutfall")

            Metrics.counter(
                "rekrutteringsbistand.statistikk.utfall.lagret",
                "utfall",
                opprettKandidatutfall.utfall.name
            ).increment()
        }
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toExtendedReport())
    }

    private data class StillingsinfoIHendelse(
        val stillingsid: String,
        val stillingskategori: Stillingskategori?
    )

    data class Kandidathendelse(
        val type: Type,
        val aktørId: String,
        val organisasjonsnummer: String,
        val kandidatlisteId: String,
        val tidspunkt: ZonedDateTime,
        val stillingsId: String?,
        val utførtAvNavIdent: String,
        val utførtAvNavKontorKode: String,
        val synligKandidat: Boolean,
        val harHullICv: Boolean?,
        val alder: Int?,
        val tilretteleggingsbehov: List<String>,
    ) {
        fun toOpprettKandidatutfall(): OpprettKandidatutfall =
            OpprettKandidatutfall(
                aktørId = aktørId,
                utfall = type.toUtfall(),
                navIdent = utførtAvNavIdent,
                navKontor = utførtAvNavKontorKode,
                kandidatlisteId = kandidatlisteId,
                stillingsId = stillingsId!!,
                synligKandidat = synligKandidat,
                harHullICv = harHullICv,
                alder = alder,
                tilretteleggingsbehov = tilretteleggingsbehov,
                tidspunktForHendelsen = tidspunkt.toOslo() // Kan ha gamle eventer med tidspunkt i UTC
            )
    }

    enum class Type(val eventName: String) {
        REGISTRER_CV_DELT("registrer-cv-delt"),
        CV_DELT_VIA_REKRUTTERINGSBISTAND("cv-delt-med-arbeidsgiver-via-rekrutteringsbistand"),
        REGISTRER_FÅTT_JOBBEN("registrer-fått-jobben"),
        FJERN_REGISTRERING_AV_CV_DELT("fjern-registrering-av-cv-delt"),
        FJERN_REGISTRERING_FÅTT_JOBBEN("fjern-registrering-fått-jobben"),
        ANNULLERT("annullert");

        fun toUtfall(): Utfall =
            when (this) {
                REGISTRER_CV_DELT -> Utfall.PRESENTERT
                CV_DELT_VIA_REKRUTTERINGSBISTAND -> Utfall.PRESENTERT
                REGISTRER_FÅTT_JOBBEN -> Utfall.FATT_JOBBEN
                FJERN_REGISTRERING_AV_CV_DELT -> Utfall.IKKE_PRESENTERT
                FJERN_REGISTRERING_FÅTT_JOBBEN -> Utfall.PRESENTERT
                ANNULLERT -> Utfall.IKKE_PRESENTERT
            }
    }
}

data class OpprettKandidatutfall(
    val aktørId: String,
    val utfall: Utfall,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String,
    val synligKandidat: Boolean,
    val harHullICv: Boolean?,
    val alder: Int?,
    val tilretteleggingsbehov: List<String>,
    val tidspunktForHendelsen: ZonedDateTime,
)