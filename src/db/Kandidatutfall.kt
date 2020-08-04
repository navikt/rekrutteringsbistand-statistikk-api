package no.nav.rekrutteringsbistand.statistikk.db

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

data class Kandidatutfall(
    val dbId: Long,
    val aktorId: String,
    val utfall: Utfall,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: UUID,
    val stillingsId: UUID,
    val tidspunkt: LocalDateTime,
    val sendtStatus: SendtStatus,
    val antallSendtForsøk: Int,
    val sisteSendtForsøk: LocalDateTime?
)

enum class SendtStatus {
    IKKE_SENDT, SENDT, KANSELLERT
}

enum class Utfall {
    IKKE_PRESENTERT, PRESENTERT, FATT_JOBBEN
}