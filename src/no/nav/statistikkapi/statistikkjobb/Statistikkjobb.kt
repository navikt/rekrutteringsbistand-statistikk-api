package no.nav.statistikkapi.statistikkjobb

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.HentStatistikk
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class Statistikkjobb(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val meterRegistry: PrometheusMeterRegistry
) {
    private val antallPresenterteKandidater = meterRegistry.gauge("antall_presenterte_kandidater", AtomicLong(0))

    val executor = Executors.newScheduledThreadPool(1)

    fun start() {
        executor.scheduleWithFixedDelay({ hentStatistikk() }, 5L, 20L, TimeUnit.SECONDS)
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