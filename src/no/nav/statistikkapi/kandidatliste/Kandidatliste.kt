package no.nav.statistikkapi.kandidatliste

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

data class Kandidatliste(
    val dbId: Long,
    val navIdent: String,
    val kandidatlisteId: UUID,
    val stillingsId: UUID,
    val erDirektemeldt: Boolean,
    val stillingOpprettetTidspunkt: ZonedDateTime,
    val stillingensPubliseringstidspunkt: ZonedDateTime,
    val antallStillinger: Int,
    val antallKandidater: Int,
    val tidspunkt: ZonedDateTime
)
