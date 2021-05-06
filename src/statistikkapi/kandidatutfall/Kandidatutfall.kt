package statistikkapi.kandidatutfall

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
    val synligKandidat: Boolean?,
    val hullICv: Boolean?,
    val tidspunkt: LocalDateTime,
    val sendtStatus: SendtStatus,
    val antallSendtForsøk: Int,
    val sisteSendtForsøk: LocalDateTime?,
    val alder: Int?,
    val tilretteleggingsbehov: List<String>
)

enum class SendtStatus {
    IKKE_SENDT, SENDT, KANSELLERT
}

enum class Utfall {
    IKKE_PRESENTERT, PRESENTERT, FATT_JOBBEN
}
