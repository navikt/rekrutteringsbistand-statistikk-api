package no.nav.statistikkapi.metrikker

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MetrikkJobb(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val kandidatlisteRepository: KandidatlisteRepository,
    private val visningKontaktinfoRepository: VisningKontaktinfoRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) {
    private val antallKandidatlisterTilknyttetStillingPerMåned = ConcurrentHashMap<String, AtomicLong>()
    private val antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned = ConcurrentHashMap<String, AtomicLong>()
    private val antallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned =
        ConcurrentHashMap<String, AtomicLong>()
    private val antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned =
        ConcurrentHashMap<String, AtomicLong>()
    private val antallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned =
        ConcurrentHashMap<String, AtomicLong>()
    private val antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned =
        ConcurrentHashMap<String, AtomicLong>()

    init {
        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_stilling_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_direktemeldt_stilling_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned().forEach {
            antallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                antallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_med_minst_en_kandidat_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }

        kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned()
            .forEach {
                antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }

    }

    private val antallPresenterteKandidater: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_presenterte_kandidater",
        AtomicLong(kandidatutfallRepository.hentAntallPresentertForAlleNavKontor().toLong())
    )

    private val antallFåttJobben: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_fått_jobben",
        AtomicLong(kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor().toLong())
    )

    private val antallKandidatlisterTilknyttetStilling: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetDirektemeldtStilling: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_direktemeldt_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetEksternStilling: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_ekstern_stilling",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger().toLong())
    )

    private val antallStillingerForEksterneStillingsannonserMedKandidatliste: AtomicLong =
        prometheusMeterRegistry.gauge(
            "antall_stillinger_for_eksterne_stillingsannonser_med_kandidatliste",
            AtomicLong(
                kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste().toLong()
            )
        )

    private val antallStillingerForDirektemeldteStillingsannonser: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_direktemeldte_stillingsannonser",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser().toLong())
    )

    private val antallStillingerForStillingsannonserMedKandidatliste: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_stillingsannonser_med_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste().toLong())
    )

    private val antallDirektemeldteStillingerMedMinstEnPresentertKandidat: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat",
        AtomicLong(kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat().toLong())
    )

    private val antallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo: AtomicLong =
        prometheusMeterRegistry.gauge(
            "antall_kandidater_i_prioritert_malgruppe_som_har_fatt_vist_sin_kontaktinfo",
            AtomicLong(
                visningKontaktinfoRepository.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()
                    .toLong()
            )
        )

    private val antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo: AtomicLong =
        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo",
            AtomicLong(
                visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()
                    .toLong()
            )
        )

    private val antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben: AtomicLong =
        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben",
            AtomicLong(
                kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()
                    .toLong()
            )
        )

    private val antallDirektemeldteStillingerSomHarTomKandidatliste: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_direktemeldte_stillinger_som_har_tom_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste().toLong())
    )

    private val antallUnikeArbeidsgivereForDirektemeldteStillinger: AtomicLong = prometheusMeterRegistry.gauge(
        "antall_unike_arbeidsgivere_for_direktemeldte_stillinger",
        AtomicLong(kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben: AtomicLong =
        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_tilknyttet_direktemeldt_stilling_der_minst_en_kandidat_fikk_jobben",
            AtomicLong(
                kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()
                    .toLong()
            )
        )

    val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private val oppdateringsintervallMinutter = 10L

    fun start() {
        val dayOfMonth = 1

        var dateTime: ZonedDateTime = ZonedDateTime.now()
        if (dateTime.getDayOfMonth() >= dayOfMonth) { // TODO Are: Alltid true, er dette en feil?
            dateTime = dateTime.plusMonths(1)
        }
        dateTime = dateTime.withDayOfMonth(dayOfMonth)
        executor.schedule(
            { initierNyeMåneder() },
            ZonedDateTime.now().until(dateTime, ChronoUnit.MILLIS),
            TimeUnit.MILLISECONDS
        )
        executor.scheduleWithFixedDelay(
            { hentStatistikkOgLogg() },
            oppdateringsintervallMinutter,
            oppdateringsintervallMinutter,
            TimeUnit.MINUTES
        )
    }

    private fun hentStatistikkOgLogg() {
        log.info("Metrikkjobben starter.")
        val start = System.currentTimeMillis()
        try {
            hentStatistikk()
            log.info("Metrikkjobben er ferdig etter ${System.currentTimeMillis() - start} ms.")
        } catch (e: Exception) {
            log.error("Metrikkjobben feilet etter ${System.currentTimeMillis() - start} ms.", e)
        }
    }

    private fun initierNyeMåneder() {
        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_stilling_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_direktemeldt_stilling_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned().forEach {
            antallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            )
        }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                antallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_med_minst_en_kandidat_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }

        kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned()
            .forEach {
                antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned[it.key] =
                    prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    )
            }
    }

    private fun hentStatistikk() {
        antallPresenterteKandidater.getAndSet(kandidatutfallRepository.hentAntallPresentertForAlleNavKontor().toLong())
        antallFåttJobben.getAndSet(kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor().toLong())
        antallKandidatlisterTilknyttetStilling.getAndSet(
            kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger().toLong()
        )
        antallKandidatlisterTilknyttetDirektemeldtStilling.getAndSet(
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger().toLong()
        )
        antallKandidatlisterTilknyttetEksternStilling.getAndSet(
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger().toLong()
        )
        antallStillingerForEksterneStillingsannonserMedKandidatliste.getAndSet(
            kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste().toLong()
        )
        antallStillingerForDirektemeldteStillingsannonser.getAndSet(
            kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser().toLong()
        )
        antallStillingerForStillingsannonserMedKandidatliste.getAndSet(
            kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste().toLong()
        )
        antallDirektemeldteStillingerMedMinstEnPresentertKandidat.getAndSet(
            kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat().toLong()
        )
        antallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo.getAndSet(
            visningKontaktinfoRepository.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo().toLong()
        )
        antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo.getAndSet(
            visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()
                .toLong()
        )
        antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben.getAndSet(
            kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben().toLong()
        )
        antallDirektemeldteStillingerSomHarTomKandidatliste.getAndSet(
            kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste().toLong()
        )
        antallUnikeArbeidsgivereForDirektemeldteStillinger.getAndSet(
            kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger().toLong()
        )
        antallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben.getAndSet(
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()
                .toLong()
        )

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach { (måned, antall) ->
            antallKandidatlisterTilknyttetStillingPerMåned[måned]?.set(antall.toLong())
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach { (måned, antall) ->
            antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[måned]?.set(antall.toLong())
        }

        kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned().forEach { (måned, antall) ->
            antallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned[måned]?.set(antall.toLong())
        }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach { (måned, antall) ->
                antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned[måned]?.set(antall.toLong())
            }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach { (måned, antall) ->
                antallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned[måned]?.set(antall.toLong())
            }

        kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned()
            .forEach { (måned, antall) ->
                antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned[måned]?.set(antall.toLong())
            }
    }
}
