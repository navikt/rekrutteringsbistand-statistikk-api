package no.nav.statistikkapi.kandidatlisteutfall

import no.nav.statistikkapi.kandidatutfall.SendtStatus
import java.time.LocalDateTime
import java.util.*

data class Kandidatlisteutfall(
    val dbId: Long,
    val utfall: UtfallKandidatliste,
    val navIdent: String,
    val kandidatlisteId: UUID,
    val stillingsId: UUID,
    val erDirektemeldt: Boolean,
    val stillingOpprettetTidspunkt: LocalDateTime,
    val antallStillinger: Int,
    val tidspunkt: LocalDateTime,
    val sendtStatus: SendtStatus,
    val antallSendtForsøk: Int,
    val sisteSendtForsøk: LocalDateTime?,
)

enum class UtfallKandidatliste {
    OPPRETTET_ELLER_OPPDATERT, LUKKET, SLETTET;

    companion object {
        fun fraEventNamePostfix(eventNamePostfix: String) =
            when (eventNamePostfix) {
                "OpprettetEllerOppdaterteKandidatliste" -> OPPRETTET_ELLER_OPPDATERT
                "SlettetStillingOgKandidatliste" -> SLETTET
                "LukketKandidatliste" -> LUKKET

                else -> throw Exception("Uventet event $eventNamePostfix for lytter")
            }
    }
}