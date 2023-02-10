package no.nav.statistikkapi.statistikkjobb

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.HentStatistikk
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class Statistikkjobb(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val meterRegistry: PrometheusMeterRegistry
) {
    private val antallPresenterteKandidater = meterRegistry.gauge("antall_presenterte_kandidater", AtomicLong(0))

    fun start() {
        log.info("Starter statistikkjobb")
    }

    fun hentStatistikk() {
        antallPresenterteKandidater.getAndSet(
            kandidatutfallRepository.hentAntallPresentert(
                HentStatistikk(
                    LocalDate.now().minusDays(7), LocalDate.now(), null
                )
            ).toLong()
        )
    }
}