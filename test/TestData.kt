import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Utfall

const val enNavIdent = "X123456"
const val enAnnenNavIdent = "Y654321"

const val etKontor1 = "1234"
const val etKontor2 = "2000"

val etKandidatutfall = OpprettKandidatutfall(
    aktørId = "10000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = "24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2",
    harHullICv = true,
    alder = 54
)

val etKandidatutfallMedUkjentHullICv = OpprettKandidatutfall(
    aktørId = "80000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enAnnenNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = "24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2",
    harHullICv = null,
    alder = null
)

