package no.nav.statistikkapi.kandidatlisteutfall

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.statistikkapi.kandidatutfall.asTextNullable
import no.nav.statistikkapi.kandidatutfall.asZonedDateTime
import no.nav.statistikkapi.secureLog
import no.nav.statistikkapi.stillinger.Stillingskategori

class OpprettetEllerOppdaterteKandidatlisteLytter(
    rapidsConnection: RapidsConnection,
    private val lagreKandidatlisteutfallOgStilling: LagreKandidatlisteutfallOgStilling,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("@slutt_av_hendelseskjede", true)
                it.rejectKey("stillingsinfo")
                it.demandValue("@event_name", "kandidat_v2.OpprettetEllerOppdaterteKandidatliste")

                it.requireKey(
                    "stillingOpprettetTidspunkt",
                    "antallStillinger",
                    "erDirektemeldt",
                    "stillingsId",
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                )
                it.interestedIn("stillingsinfo.stillingskategori")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingOpprettetTidspunkt = packet["stillingOpprettetTidspunkt"].asZonedDateTime()
        val antallStillinger = packet["antallStillinger"].asInt()
        val erDirektemeldt = packet["erDirektemeldt"].asBoolean()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val kandidatlisteId = packet["kandidatlisteId"].asText()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val stillingsId = packet["stillingsId"].asTextNullable()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asText()
        val stillingskategori = packet["stillingsinfo.stillingskategori"].asTextNullable()

        secureLog.info(
            """
                stillingOpprettetTidspunkt: $stillingOpprettetTidspunkt
                antallStillinger: $antallStillinger
                erDirektemeldt: $erDirektemeldt
                organisasjonsnummer: $organisasjonsnummer
                kandidatlisteId: $kandidatlisteId
                tidspunkt: $tidspunkt
                stillingsId: $stillingsId
                utførtAvNavIdent: $utførtAvNavIdent
                stillingskategori: $stillingskategori
            """.trimIndent()
        )

        val opprettKandidatlisteutfall = OpprettKandidatlisteutfall(
            stillingOpprettetTidspunkt = stillingOpprettetTidspunkt,
            antallStillinger = antallStillinger,
            erDirektemeldt = erDirektemeldt,
            kandidatlisteId = kandidatlisteId,
            tidspunktForHendelsen = tidspunkt,
            stillingsId = stillingsId,
            navIdent = utførtAvNavIdent,
            utfall = UtfallKandidatliste.OPPRETTET_ELLER_OPPDATERT
        )

        lagreKandidatlisteutfallOgStilling.lagreKandidatlsiteutfallOgStilling(
            kandidatlisteutfall = opprettKandidatlisteutfall,
            stillingsId = stillingsId,
            stillingskategori = Stillingskategori.fraNavn(stillingskategori)
        )

        prometheusMeterRegistry.counter(
            "rekrutteringsbistand.statistikk.kandidatlisteutfall.lagret",
            "utfall", opprettKandidatlisteutfall.utfall.name
        ).increment()
    }
}