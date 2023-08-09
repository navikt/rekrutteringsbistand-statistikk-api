package no.nav.statistikkapi

import no.nav.statistikkapi.kandidatutfall.OpprettKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Utfall
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

const val enNavIdent = "X123456"
const val enAnnenNavIdent = "Y654321"

const val etKontor1 = "1234"
const val etKontor2 = "2000"

val aktørId1 = "100000000001"
val aktørId2 = "100000000002"

val enStillingsId: UUID = UUID.fromString("24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2")

val today: LocalDate = nowOslo().toLocalDate()

fun etUtfall(
    aktørId: String = "10000254879658",
    utfall: Utfall = Utfall.PRESENTERT,
    navIdent: String = enNavIdent,
    navKontor: String = etKontor1,
    kandidatlisteId: String = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId: String = enStillingsId.toString(),
    synligKandidat: Boolean = true,
    harHullICv: Boolean? = true,
    alder: Int? = 54,
    tidspunktForHendelsen: ZonedDateTime = nowOslo(),
    innsatsbehov: String? = "BFORM",
    hovedmål: String? = "SKAFFERA"
): OpprettKandidatutfall = OpprettKandidatutfall(
    aktørId = aktørId,
    utfall = utfall,
    navIdent = navIdent,
    navKontor = navKontor,
    kandidatlisteId = kandidatlisteId,
    stillingsId = stillingsId,
    synligKandidat = synligKandidat,
    harHullICv = harHullICv,
    alder = alder,
    tidspunktForHendelsen = tidspunktForHendelsen,
    innsatsbehov = innsatsbehov,
    hovedmål = hovedmål
)

val etKandidatutfall = etUtfall()

val etKandidatutfallIPrioritertMålgruppeMedUkjentHullICv = OpprettKandidatutfall(
    aktørId = "80000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enAnnenNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = enStillingsId.toString(),
    synligKandidat = true,
    harHullICv = null,
    alder = null,
    tidspunktForHendelsen = nowOslo(),
    innsatsbehov = "BFORM",
    hovedmål = "SKAFFERA"
)

val etKandidatutfallIkkeIPrioritertMålgruppe = OpprettKandidatutfall(
    aktørId = "10000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = enStillingsId.toString(),
    synligKandidat = true,
    harHullICv = false,
    alder = 35,
    tidspunktForHendelsen = nowOslo(),
    innsatsbehov = "IKVAL",
    hovedmål = "SKAFFERA"
)

