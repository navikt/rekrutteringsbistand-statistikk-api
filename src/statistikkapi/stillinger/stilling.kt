package statistikkapi.stillinger

import java.time.LocalDate

data class Stilling(
    val uuid: String,
    val publisert: LocalDate,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupperTags: List<PrioriterteMålgrupperTag>,
    val tiltakVirkemidlerTags: List<TiltakVirkemiddelTag>
)

enum class InkluderingTag {
    ARBEIDSTID,
    ARBEIDSMILJØ,
    FYSISK,
    GRUNNLEGGENDE
}

enum class PrioriterteMålgrupperTag {
    UNGE_UNDER_30,
    SENIORER_OVER_45,
    KOMMER_FRA_LAND_UTENFOR_EØS,
    HULL_I_CV,
    LITE_ELLER_INGEN_UTDANNING,
    LITE_ELLER_INGEN_ARBEIDSERFARING
}

enum class TiltakVirkemiddelTag {
    LØNNSTILSKUDD,
    MENTORTILSKUDD,
    LÆRLINGPLASS
}
