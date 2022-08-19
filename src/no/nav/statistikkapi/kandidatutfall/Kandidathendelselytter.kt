package no.nav.statistikkapi.kandidatutfall

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.*
import no.nav.statistikkapi.Cluster.DEV_FSS
import no.nav.statistikkapi.Cluster.LOKAL
import java.time.ZoneId
import java.time.ZonedDateTime

class Kandidathendelselytter(rapidsConnection: RapidsConnection, private val repo: KandidatutfallRepository) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kandidat.cv-delt-med-arbeidsgiver-via-rekrutteringsbistand")
                it.interestedIn("kandidathendelse")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val kandidathendelse: Kandidathendelse =
            objectMapper.treeToValue(packet["kandidathendelse"], Kandidathendelse::class.java)

        if(kanStolePåDatakvaliteten(kandidathendelse)) {
            val opprettKandidatutfall: OpprettKandidatutfall = kandidathendelse.toOpprettKandidatutfall()
            log.info("Har lest kandidatutfall fra Kafka")
            repo.lagreUtfallIdempotent(opprettKandidatutfall)
        } else {
            log.info("Lagrer ikke kandidatutfall fordi vi ikke stoler på kvaliteten i meldingen")
        }
    }
    
    fun kanStolePåDatakvaliteten(kandidathendelse: Kandidathendelse): Boolean {
        val kunMeldingerEtter: ZonedDateTime = ZonedDateTime.of(
            2022, 8, 19, 11,
            0,0,0,
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
        val stillingsId: String,
        val utførtAvNavIdent: String,
        val utførtAvNavKontorKode: String,
        val synligKandidat: Boolean,
        val harHullICv: Boolean,
        val alder: Int,
        val tilretteleggingsbehov: List<String>,
    ) {
        fun toOpprettKandidatutfall(): OpprettKandidatutfall =
            OpprettKandidatutfall(
                aktørId = aktørId,
                utfall = type.toUtfall(),
                navIdent = utførtAvNavIdent,
                navKontor = utførtAvNavKontorKode,
                kandidatlisteId = kandidatlisteId,
                stillingsId = stillingsId,
                synligKandidat = synligKandidat,
                harHullICv = harHullICv,
                alder = alder,
                tilretteleggingsbehov = tilretteleggingsbehov,
                tidspunktForHendelsen = tidspunkt.toOslo() // Kan ha gamle eventer med tidspunkt i UTC
            )
    }


    enum class Type(private val eventNamePostfix: String) {
        CV_DELT_UTENFOR_REKRUTTERINGSBISTAND("cv-delt-med-arbeidsgiver-utenfor-rekrutteringsbistand"),
        CV_DELT_VIA_REKRUTTERINGSBISTAND("cv-delt-med-arbeidsgiver-via-rekrutteringsbistand");

        fun toUtfall(): Utfall =
            when (this) {
                CV_DELT_UTENFOR_REKRUTTERINGSBISTAND -> Utfall.PRESENTERT
                CV_DELT_VIA_REKRUTTERINGSBISTAND -> Utfall.PRESENTERT
            }

    }
}
