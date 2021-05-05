package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository

class Datagrunnlag(val utfallElementPresentert: List<KandidatutfallRepository.UtfallElement>, val utfallElementFåttJobben: List<KandidatutfallRepository.UtfallElement>) {

}
