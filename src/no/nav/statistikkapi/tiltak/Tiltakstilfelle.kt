package no.nav.statistikkapi.tiltak

data class Tiltakstilfelle(val aktørId: String, val tiltakstype: Tiltakstype)
enum class Tiltakstype{
    ARBEIDSTRENING, LØNNSTILSKUDD, MENTORORDNING, ANNET
}

fun String.tilTiltakstype() = when(this) {
    "ARBEIDSTRENING" -> Tiltakstype.ARBEIDSTRENING
    "MIDLERTIDIG_LONNSTILSKUDD" -> Tiltakstype.LØNNSTILSKUDD
    "VARIG_LONNSTILSKUDD" -> Tiltakstype.LØNNSTILSKUDD
    "MENTOR" -> Tiltakstype.MENTORORDNING
    else -> Tiltakstype.ANNET
}