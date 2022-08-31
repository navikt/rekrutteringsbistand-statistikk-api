package no.nav.statistikkapi.kandidatutfall

import io.ktor.server.routing.*
import io.micrometer.core.instrument.Metrics
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.*
import no.nav.statistikkapi.kafka.DatavarehusKafkaProducer
import no.nav.statistikkapi.kafka.KafkaTilDataverehusScheduler
import no.nav.statistikkapi.kafka.hentUsendteUtfallOgSendPåKafka
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingService
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.sql.DataSource

class Kandidathendelselytter(
    rapidsConnection: RapidsConnection,
    private val repo: KandidatutfallRepository,
    dataSource: DataSource,
    elasticSearchKlient: ElasticSearchKlient,
    datavarehusKafkaProducer: DatavarehusKafkaProducer
) :
    River.PacketListener {

    private val kjørSchedulerAsync: () -> Unit

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny(
                    key = "@event_name",
                    values = Type.values().map { "kandidat.${it.eventName}" }
                )
                it.interestedIn("kandidathendelse")
            }
        }.register(this)

        val stillingRepository = StillingRepository(dataSource)
        val kandidatutfallRepository = KandidatutfallRepository(dataSource)
        val stillingService = StillingService(elasticSearchKlient, stillingRepository)
        val sendKafkaMelding: Runnable =
            hentUsendteUtfallOgSendPåKafka(kandidatutfallRepository, datavarehusKafkaProducer, stillingService)
        val datavarehusScheduler = KafkaTilDataverehusScheduler(dataSource, sendKafkaMelding)

        datavarehusScheduler.kjørPeriodisk()

        kjørSchedulerAsync = { datavarehusScheduler.kjørEnGangAsync() }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidathendelse: Kandidathendelse =
            objectMapper.treeToValue(packet["kandidathendelse"], Kandidathendelse::class.java)

        log.info("Har mottatt kandidathendelse")

        if (kandidathendelse.stillingsId == null) {
            log.info("Behandler ikke melding fordi den er uten stilingsId")
            return
        } else if (!kanStolePåDatakvaliteten(kandidathendelse)) {
            log.info("Behandler ikke melding fordi vi ikke kan stole på datakvaliteten")
            return
        }

        val opprettKandidatutfall: OpprettKandidatutfall = kandidathendelse.toOpprettKandidatutfall()

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
        kjørSchedulerAsync()
    }

    fun kanStolePåDatakvaliteten(kandidathendelse: Kandidathendelse): Boolean {
        val kunMeldingerEtter: ZonedDateTime = ZonedDateTime.of(
            2022, 8, 19, 11,
            0, 0, 0,
            ZoneId.of("Europe/Oslo")
        )
        return kandidathendelse.tidspunkt.isAfter(kunMeldingerEtter)
    }

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