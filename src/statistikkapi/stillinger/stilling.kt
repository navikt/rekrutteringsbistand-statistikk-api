package no.nav.statistikkapi.stillinger

import java.time.LocalDateTime

data class Stilling(
    val uuid: String,
    val opprettet: LocalDateTime,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerVirkemidler: List<TiltakEllerVirkemiddelTag>,
    val tidspunkt: LocalDateTime
) {
    infix fun `er ulik`(stillingFraElasticSearch: ElasticSearchStilling) = !(this `er lik` stillingFraElasticSearch)
    infix fun `er lik`(stillingFraElasticSearch: ElasticSearchStilling) =
        uuid == stillingFraElasticSearch.uuid &&
                opprettet == stillingFraElasticSearch.opprettet &&
                publisert == stillingFraElasticSearch.publisert &&
                inkluderingsmuligheter == stillingFraElasticSearch.inkluderingsmuligheter &&
                prioriterteMålgrupper == stillingFraElasticSearch.prioriterteMålgrupper &&
                tiltakEllerVirkemidler == stillingFraElasticSearch.tiltakEllerEllerVirkemidler
}

data class ElasticSearchStilling(
    val uuid: String,
    val opprettet: LocalDateTime,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerEllerVirkemidler: List<TiltakEllerVirkemiddelTag>
)

enum class InkluderingTag {
    ARBEIDSTID,
    ARBEIDSMILJØ,
    FYSISK,
    GRUNNLEGGENDE;
    companion object {
        private val prefix = "INKLUDERING__"
        fun erGyldig(tagNavn: String) = tagNavn.startsWith(prefix)
        fun fraNavn(tagNavn: String) = valueOf(tagNavn.removePrefix(prefix))
    }
}

enum class PrioriterteMålgrupperTag {
    UNGE_UNDER_30,
    SENIORER_OVER_45,
    KOMMER_FRA_LAND_UTENFOR_EØS,
    HULL_I_CV_EN,
    LITE_ELLER_INGEN_UTDANNING,
    LITE_ELLER_INGEN_ARBEIDSERFARING;
    companion object {
        private val prefix = "PRIORITERT_MÅLGRUPPE__"
        fun erGyldig(tagNavn: String) = tagNavn.startsWith(prefix)
        fun fraNavn(tagNavn: String) = valueOf(tagNavn.removePrefix(prefix))
    }
}

enum class TiltakEllerVirkemiddelTag {
    LØNNSTILSKUDD,
    MENTORTILSKUDD,
    LÆRLINGPLASS;
    companion object {
        private val prefix = "TILTAK_ELLER_VIRKEMIDDEL__"
        fun erGyldig(tagNavn: String) = tagNavn.startsWith(prefix)
        fun fraNavn(tagNavn: String) = valueOf(tagNavn.removePrefix(prefix))
    }
}
