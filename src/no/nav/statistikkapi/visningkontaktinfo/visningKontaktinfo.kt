package no.nav.statistikkapi.visningkontaktinfo

import java.time.ZonedDateTime
import java.util.*

data class VisningKontaktinfo(
    val aktørId: String,
    val stillingId: UUID,
    val tidspunkt: ZonedDateTime
)