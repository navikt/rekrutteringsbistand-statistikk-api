package no.nav.statistikkapi.kandidatutfall

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
    val innsatsbehov: String?,
    val hovedmål: String?,
    val tidspunkt: LocalDateTime,
    val sendtStatus: SendtStatus,
    val antallSendtForsøk: Int,
    val sisteSendtForsøk: LocalDateTime?,
    val alder: Int?,
)

enum class SendtStatus {
    IKKE_SENDT, SENDT, KANSELLERT
}

enum class Utfall {
    IKKE_PRESENTERT, PRESENTERT, FATT_JOBBEN;

    companion object {
        fun fraEventNamePostfix(eventNamePostfix: String) =
            when (eventNamePostfix) {
                "RegistrertDeltCv" -> PRESENTERT
                "RegistrertFåttJobben" -> FATT_JOBBEN

                else -> throw Exception("Uventet event $eventNamePostfix for lytter")
            }
    }
}

enum class Innsatsgruppe {
    BATT, // spesielt tilpasset innsats
    BFORM, // situasjonsbestemt innsats
    VARIG, // varig tilpasset eller gradert varig tilpasset innsats
    IKVAL // standard innsats
}
