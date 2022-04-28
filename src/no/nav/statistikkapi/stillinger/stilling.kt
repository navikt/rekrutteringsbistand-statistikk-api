package no.nav.statistikkapi.stillinger

import no.nav.rekrutteringsbistand.AvroStillingskategori
import java.time.LocalDateTime

data class Stilling(
    val uuid: String,
    val opprettet: LocalDateTime,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerVirkemidler: List<TiltakEllerVirkemiddelTag>,
    val tidspunkt: LocalDateTime,
    val stillingskategori: Stillingskategori
) {
    infix fun `er ulik`(stillingFraElasticSearch: ElasticSearchStilling) = !(this `er lik` stillingFraElasticSearch)
    infix fun `er lik`(stillingFraElasticSearch: ElasticSearchStilling) =
        uuid == stillingFraElasticSearch.uuid &&
                opprettet == stillingFraElasticSearch.opprettet &&
                publisert == stillingFraElasticSearch.publisert &&
                inkluderingsmuligheter == stillingFraElasticSearch.inkluderingsmuligheter &&
                prioriterteMålgrupper == stillingFraElasticSearch.prioriterteMålgrupper &&
                tiltakEllerVirkemidler == stillingFraElasticSearch.tiltakEllerEllerVirkemidler &&
                stillingskategori == stillingFraElasticSearch.stillingskategori
}

data class ElasticSearchStilling(
    val uuid: String,
    val opprettet: LocalDateTime,
    val publisert: LocalDateTime,
    val inkluderingsmuligheter: List<InkluderingTag>,
    val prioriterteMålgrupper: List<PrioriterteMålgrupperTag>,
    val tiltakEllerEllerVirkemidler: List<TiltakEllerVirkemiddelTag>,
    val stillingskategori: Stillingskategori
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

enum class Stillingskategori {
    STILLING, FORMIDLING, ARBEIDSTRENING, JOBBMESSE;

    fun tilAvro() = when (this) {
        STILLING -> AvroStillingskategori.STILLING
        FORMIDLING -> AvroStillingskategori.FORMIDLING
        ARBEIDSTRENING -> AvroStillingskategori.ARBEIDSTRENING
        JOBBMESSE -> AvroStillingskategori.JOBBMESSE
    }

    companion object {
        fun fraElasticSearch(s: String?) = if (s == null) STILLING else valueOf(s)
        fun fraDatabase(s: String?) = if (s == null) STILLING else valueOf(s)
    }
}
