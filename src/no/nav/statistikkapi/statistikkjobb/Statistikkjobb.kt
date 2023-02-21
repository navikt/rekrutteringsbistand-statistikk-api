package no.nav.statistikkapi.statistikkjobb

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class Statistikkjobb(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val kandidatlisteRepository: KandidatlisteRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) {
    private val antallPresenterteKandidater = prometheusMeterRegistry.gauge(
        "antall_presenterte_kandidater",
        AtomicLong(kandidatutfallRepository.hentAntallPresentertForAlleNavKontor().toLong())
    )
    private val antallFåttJobben = prometheusMeterRegistry.gauge(
        "antall_fått_jobben",
        AtomicLong(kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor().toLong())
    )

    val executor = Executors.newScheduledThreadPool(1)

    fun start() {
        executor.scheduleWithFixedDelay({ hentStatistikk() }, 5L, 20L, TimeUnit.SECONDS)
    }

    private fun hentStatistikk() {
        antallPresenterteKandidater.getAndSet(kandidatutfallRepository.hentAntallPresentertForAlleNavKontor().toLong())
        antallFåttJobben.getAndSet(kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor().toLong())
    }
}