package no.nav.statistikkapi.statistikkjobb

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.log

class Statistikkjobb(
    kandidatutfallRepository: KandidatutfallRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    fun start() {
        log.info("Starter statistikkjobb")
    }
}