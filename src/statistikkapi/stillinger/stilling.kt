package statistikkapi.stillinger

import java.time.LocalDateTime

data class Stilling(
    val id: Long,
    val uuid: String,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerEllerVirkemidler: List<TiltakEllerVirkemiddelTag>,
    val tidspunkt: LocalDateTime
)

data class ElasticSearchStilling(
    val uuid: String,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerEllerVirkemidler: List<TiltakEllerVirkemiddelTag>
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

enum class TiltakEllerVirkemiddelTag {
    LØNNSTILSKUDD,
    MENTORTILSKUDD,
    LÆRLINGPLASS
}
