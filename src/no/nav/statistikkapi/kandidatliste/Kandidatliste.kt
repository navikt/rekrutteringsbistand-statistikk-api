package no.nav.statistikkapi.kandidatliste

import no.nav.statistikkapi.kandidatutfall.SendtStatus
import java.time.LocalDateTime
import java.util.*

data class Kandidatliste(
    val dbId: Long,
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
