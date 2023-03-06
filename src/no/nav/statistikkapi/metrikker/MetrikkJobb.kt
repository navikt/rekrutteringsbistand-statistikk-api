package no.nav.statistikkapi.metrikker

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MetrikkJobb(
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

    private val antallKandidatlisterTilknyttetStilling = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetDirektemeldtStilling = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_direktemeldt_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetEksternStilling = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_ekstern_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger().toLong())
    )

    private val antallStillingerForAlleEksterneStillingsannonserMedKandidatliste = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_alle_eksterne_stillingsannonser_med_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForAlleEksterneStillingsannonserMedKandidatliste().toLong())
    )

    private val antallStillingerForAlleDirektemeldteStillingsannonser = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_alle_direktemeldte_stillingsannonser",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForAlleDirektemeldteStillingsannonser().toLong())
    )

    private val antallStillingerForAlleStillingsannonserMedKandidatliste = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_alle_stillingsannonser_med_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForAlleStillingsannonserMedKandidatliste().toLong())
    )

    val executor = Executors.newScheduledThreadPool(1)

    fun start() {
        executor.scheduleWithFixedDelay({ hentStatistikk() }, 5L, 20L, TimeUnit.SECONDS)
    }

    private fun hentStatistikk() {
        antallPresenterteKandidater.getAndSet(kandidatutfallRepository.hentAntallPresentertForAlleNavKontor().toLong())
        antallFåttJobben.getAndSet(kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor().toLong())
        antallKandidatlisterTilknyttetStilling.getAndSet(kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger().toLong())
        antallKandidatlisterTilknyttetDirektemeldtStilling.getAndSet(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger().toLong())
        antallKandidatlisterTilknyttetEksternStilling.getAndSet(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger().toLong())
        antallStillingerForAlleEksterneStillingsannonserMedKandidatliste.getAndSet(kandidatlisteRepository.hentAntallStillingerForAlleEksterneStillingsannonserMedKandidatliste().toLong())
        antallStillingerForAlleDirektemeldteStillingsannonser.getAndSet(kandidatlisteRepository.hentAntallStillingerForAlleDirektemeldteStillingsannonser().toLong())
        antallStillingerForAlleStillingsannonserMedKandidatliste.getAndSet(kandidatlisteRepository.hentAntallStillingerForAlleStillingsannonserMedKandidatliste().toLong())
    }
}