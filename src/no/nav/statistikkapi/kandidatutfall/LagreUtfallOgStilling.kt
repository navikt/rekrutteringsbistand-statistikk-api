package no.nav.statistikkapi.kandidatutfall

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class LagreUtfallOgStilling(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val stillingRepository: StillingRepository
) {

    fun lagreUtfallOgStilling(
        kandidatutfall: OpprettKandidatutfall,
        stillingsid: String,
        stillingskategori: Stillingskategori
    ) {
        stillingRepository.lagreStilling(stillingsid, stillingskategori)
        if (kandidatutfallRepository.kandidatutfallAlleredeLagret(kandidatutfall)) {
            log.info("Lagrer ikke fordi vi har lagret samme utfall tidligere")
        } else if (kandidatutfallRepository.hentSisteUtfallForKandidatIKandidatliste(kandidatutfall) == kandidatutfall.utfall) {
            log.info("Lagrer ikke fordi siste kandidatutfall for samme kandidat og kandidatliste har likt utfall")
        } else {
            kandidatutfallRepository.lagreUtfall(kandidatutfall)
            log.info("Lagrer kandidathendelse som kandidatutfall")

            Metrics.counter(
                "rekrutteringsbistand.statistikk.utfall.lagret",
                "utfall",
                kandidatutfall.utfall.name
            ).increment()
        }
    }
}

data class OpprettKandidatutfall(
    val aktørId: String,
    val utfall: Utfall,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String,
    val synligKandidat: Boolean,
    val harHullICv: Boolean?,
    val innsatsbehov: String?,
    val hovedmål: String?,
    val alder: Int?,
    val tilretteleggingsbehov: List<String>,
    val tidspunktForHendelsen: ZonedDateTime,
)