package no.nav.statistikkapi

import no.nav.statistikkapi.kandidatutfall.OpprettKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Utfall
import java.util.*

const val enNavIdent = "X123456"
const val enAnnenNavIdent = "Y654321"

const val etKontor1 = "1234"
const val etKontor2 = "2000"

val enStillingsId = UUID.fromString("24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2")


val etKandidatutfall = OpprettKandidatutfall(
    aktørId = "10000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = enStillingsId.toString(),
    synligKandidat = true,
    harHullICv = true,
    alder = 35,
    tidspunktForHendelsen = nowOslo(),
    innsatsbehov = "IKVAL",
    hovedmål = "SKAFFERA"
)


// TODO Are: Utfall som ikke er prioritert målgruppe bør være default, dvs. vi bør kunne gjenbruke "etKandidatutfall" ovenfor, men noen tester ble røde da jeg prøvde. Hvis det er viktig og relevant for en test bør det være asserts i testmetoden som verifiserer alder og innsatsgruppe der.
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

val aktørId1 = "100000000001"
val aktørId2 = "100000000002"
