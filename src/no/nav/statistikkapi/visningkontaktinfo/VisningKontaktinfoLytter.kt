package no.nav.statistikkapi.visningkontaktinfo

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.statistikkapi.json.asUUID
import no.nav.statistikkapi.json.asZonedDateTime
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.rapidsandrivers.requireValueIfPresent

class VisningKontaktinfoLytter(
    rapidsConnection: RapidsConnection,
    private val repository: VisningKontaktinfoRepository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { packet ->
                packet.requireValue("@event_name", "arbeidsgiversKandidatliste.VisningKontaktinfo")
                packet.requireValueIfPresent("@slutt_av_hendelseskjede", false)
            }
            validate {
                it.requireKey("aktørId", "stillingsId", "tidspunkt")
                it.interestedIn("@event_name", "@slutt_av_hendelseskjede")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
         val aktørId = packet["aktørId"].asString()
         val stillingsId = packet["stillingsId"].asUUID()
         val tidspunkt = packet["tidspunkt"].asZonedDateTime()

        val alleredeLagret = repository.harAlleredeBlittLagret(aktørId, stillingsId, tidspunkt)

        if (alleredeLagret) {
            log.info("Melding om visning av kontaktinfo har allerede blitt lagret, så ignorerer melding")
        } else {
            repository.lagre(aktørId, stillingsId, tidspunkt)
        }

        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }
}
