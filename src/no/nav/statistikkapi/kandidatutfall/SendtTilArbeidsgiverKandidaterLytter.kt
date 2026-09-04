package no.nav.statistikkapi.kandidatutfall

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.json.asTextNullable
import no.nav.statistikkapi.json.asZonedDateTime
import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.stillinger.Stillingskategori
import tools.jackson.databind.JsonNode

class SendtTilArbeidsgiverKandidaterLytter(
    rapidsConnection: RapidsConnection,
    private val lagreUtfallOgStilling: LagreUtfallOgStilling,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) : River.PacketListener {
    private val secureLog = secure(log)

    init {
        River(rapidsConnection).apply {
            precondition { packet ->
                packet.requireValue("@event_name", "kandidat_v2.DelCvMedArbeidsgiver")
                packet.forbidValue("@slutt_av_hendelseskjede", true)
            }
            validate {
                it.requireKey(
                    "stillingsId",
                    "organisasjonsnummer",
                    "kandidatlisteId",
                    "tidspunkt",
                    "utførtAvNavIdent",
                    "utførtAvNavKontorKode",
                    "arbeidsgiversEpostadresser",
                    "meldingTilArbeidsgiver",
                    "kandidater"
                )
                it.require("kandidater") { kandidater ->
                    require(kandidater.isObject) { "kandidater må være et JSON-objekt" }

                    kandidater.properties().forEach { (_, kandidat) ->
                        require(kandidat.isObject) { "hver kandidat må være et JSON-objekt" }
                        require(kandidat["harHullICv"].isBoolean) { "hver kandidat må ha boolsk harHullICv" }
                        require(kandidat["alder"].isInt) { "hver kandidat må ha heltallig alder" }
                        require(kandidat["innsatsbehov"].isString) { "hver kandidat må ha tekstlig innsatsbehov" }
                    }
                }
                it.interestedIn(
                    "stillingsinfo.stillingskategori"
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
        val stillingsId: String = packet["stillingsId"].asTextNullable() ?: run {
            log.warn("Denne koden burde aldri bli kjørt. Behandler ikke melding fordi den er uten stillingsId. stilingsId burde vært til stede pga filterlogikken i River.validate.requireKey.")
            return
        }
        val stillingskategori = packet["stillingsinfo.stillingskategori"].asTextNullable()
        val organisasjonsnummer = packet["organisasjonsnummer"].asString()
        val kandidatlisteId = packet["kandidatlisteId"].asString()
        val tidspunkt = packet["tidspunkt"].asZonedDateTime()
        val utførtAvNavIdent = packet["utførtAvNavIdent"].asString()
        val utførtAvNavKontorKode = packet["utførtAvNavKontorKode"].asString()
        val arbeidsgiversEpostadresser = packet["arbeidsgiversEpostadresser"].toList().map(JsonNode::asString)
        val meldingTilArbeidsgiver = packet["meldingTilArbeidsgiver"].asString()

        packet["kandidater"].properties().forEach { (aktørId, node) ->
            val harHullICv = node["harHullICv"].booleanValue()
            val alder = node["alder"].intValue()
            val innsatsbehov = node["innsatsbehov"].asString()
            val hovedmål = node["hovedmål"].asTextNullable()

            secureLog.info(
                """
            stillingsId: $stillingsId
            stillingskategori: $stillingskategori
            organisasjonsnummer: $organisasjonsnummer
            kandidatlisteId: $kandidatlisteId
            tidspunkt: $tidspunkt
            stillingsId: $stillingsId
            utførtAvNavIdent: $utførtAvNavIdent
            utførtAvNavKontorKode: $utførtAvNavKontorKode
            arbeidsgiversEpostadresser: $arbeidsgiversEpostadresser
            meldingTilArbeidsgiver: $meldingTilArbeidsgiver
            aktørId: $aktørId
            harHullICv: $harHullICv
            alder: $alder
            innsatsbehov: $innsatsbehov
            hovedmål: $hovedmål
            """.trimIndent()
            )

            val opprettKandidatutfall = OpprettKandidatutfall(
                aktørId = aktørId,
                utfall = Utfall.PRESENTERT,
                navIdent = utførtAvNavIdent,
                navKontor = utførtAvNavKontorKode,
                kandidatlisteId = kandidatlisteId,
                stillingsId = stillingsId,
                synligKandidat = true,
                harHullICv = harHullICv,
                alder = alder,
                tidspunktForHendelsen = tidspunkt,
                innsatsbehov = innsatsbehov,
                hovedmål = hovedmål,
                rekrutteringstreffId = null,
            )

            lagreUtfallOgStilling.lagreUtfallOgStilling(
                kandidatutfall = opprettKandidatutfall,
                stillingsid = stillingsId,
                stillingskategori = Stillingskategori.fraNavn(stillingskategori)
            )

            packet["@slutt_av_hendelseskjede"] = true
            context.publish(packet.toJson())
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av melding\n$problems")
    }
}
