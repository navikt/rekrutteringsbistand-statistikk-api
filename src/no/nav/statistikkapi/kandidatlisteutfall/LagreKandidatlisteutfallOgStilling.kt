package no.nav.statistikkapi.kandidatlisteutfall

import io.micrometer.core.instrument.Metrics
import no.nav.statistikkapi.log
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.Stillingskategori
import java.time.ZonedDateTime

class LagreKandidatlisteutfallOgStilling(
    private val kandidatlisteutfallRepository: KandidatlisteutfallRepository,
    private val stillingRepository: StillingRepository
) {

    fun lagreKandidatlsiteutfallOgStilling(
        kandidatlisteutfall: OpprettKandidatlisteutfall,
        stillingsId: String,
        stillingskategori: Stillingskategori
    ) {
        stillingRepository.lagreStilling(stillingsId, stillingskategori)
        lagreKandidatlisteutfall(kandidatlisteutfall)
    }

    fun lagreKandidatlisteutfall(
        kandidatlisteutfall: OpprettKandidatlisteutfall
    ) {
        if (kandidatlisteutfallRepository.kandidatlisteutfallAlleredeLagret(kandidatlisteutfall)) {
            log.info("Lagrer ikke fordi vi har lagret samme kandidatlisteutfall tidligere")
        } else if (kandidatlisteutfallRepository.hentSisteUtfallKandidatlisteForKandidatliste(kandidatlisteutfall) == kandidatlisteutfall.utfall) {
            log.info("Lagrer ikke fordi siste kandidatlisteutfall for samme kandidatliste har likt utfall")
        } else {
            kandidatlisteutfallRepository.lagreKandidatlisteutfall(kandidatlisteutfall)
            log.info("Lagrer kandidatlistehendelse som kandidatlisteutfall")

            Metrics.counter(
                "rekrutteringsbistand.statistikk.kandidatlisteutfall.lagret",
                "utfall",
                kandidatlisteutfall.utfall.name
            ).increment()
        }
    }
}

data class OpprettKandidatlisteutfall(
    val stillingOpprettetTidspunkt: ZonedDateTime,
    val antallStillinger: Int,
    val erDirektemeldt: Boolean,
    val kandidatlisteId: String,
    val tidspunktForHendelsen: ZonedDateTime,
    val stillingsId: String,
    val navIdent: String,
    val utfall: UtfallKandidatliste
)