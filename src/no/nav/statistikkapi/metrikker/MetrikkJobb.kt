package no.nav.statistikkapi.metrikker

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
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
    private val antallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned = ConcurrentHashMap<String, AtomicLong>()

    init {
        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_stilling_per_maned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            ) as AtomicLong
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_direktemeldt_stilling_per_maaned",
                Tags.of("maaned", it.key),
                AtomicLong(it.value.toLong())
            ) as AtomicLong
        }

    }

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

    private val antallStillingerForEksterneStillingsannonserMedKandidatliste = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_eksterne_stillingsannonser_med_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste().toLong())
    )

    private val antallStillingerForDirektemeldteStillingsannonser = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_direktemeldte_stillingsannonser",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser().toLong())
    )

    private val antallStillingerForStillingsannonserMedKandidatliste = prometheusMeterRegistry.gauge(
        "antall_stillinger_for_stillingsannonser_med_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste().toLong())
    )

    private val antallDirektemeldteStillingerMedMinstEnPresentertKandidat = prometheusMeterRegistry.gauge(
        "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat",
        AtomicLong(kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat().toLong())
    )

    private val antallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo = prometheusMeterRegistry.gauge(
        "antall_kandidater_i_prioritert_malgruppe_som_har_fatt_vist_sin_kontaktinfo",
        AtomicLong(visningKontaktinfoRepository.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo().toLong())
    )

    private val antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo  = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo",
        AtomicLong(visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo().toLong())
    )

    private val antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben().toLong())
    )

    private val antallDirektemeldteStillingerSomHarTomKandidatliste = prometheusMeterRegistry.gauge(
        "antall_direktemeldte_stillinger_som_har_tom_kandidatliste",
        AtomicLong(kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste().toLong())
    )

    private val antallUnikeArbeidsgivereForDirektemeldteStillinger = prometheusMeterRegistry.gauge(
        "antall_unike_arbeidsgivere_for_direktemeldte_stillinger",
        AtomicLong(kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger().toLong())
    )

    private val antallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben = prometheusMeterRegistry.gauge(
        "antall_kandidatlister_tilknyttet_direktemeldt_stilling_der_minst_en_kandidat_fikk_jobben",
        AtomicLong(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben().toLong())
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
        antallStillingerForEksterneStillingsannonserMedKandidatliste.getAndSet(kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste().toLong())
        antallStillingerForDirektemeldteStillingsannonser.getAndSet(kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser().toLong())
        antallStillingerForStillingsannonserMedKandidatliste.getAndSet(kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste().toLong())
        antallDirektemeldteStillingerMedMinstEnPresentertKandidat.getAndSet(kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat().toLong())
        antallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo.getAndSet(visningKontaktinfoRepository.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo().toLong())
        antallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo.getAndSet(visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo().toLong())
        antallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben.getAndSet(kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben().toLong())
        antallDirektemeldteStillingerSomHarTomKandidatliste.getAndSet(kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste().toLong())
        antallUnikeArbeidsgivereForDirektemeldteStillinger.getAndSet(kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger().toLong())
        antallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben.getAndSet(kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben().toLong())

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetStillingPerMåned.keys.forEach { k ->
                if (k == it.key) {
                    antallKandidatlisterTilknyttetStillingPerMåned[k]?.getAndSet(it.value.toLong())
                } else {
                    antallKandidatlisterTilknyttetStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_tilknyttet_stilling_per_maned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    ) as AtomicLong
                }
            }
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach {
            antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned.keys.forEach {k ->
                if (k == it.key) {
                    antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[k]?.getAndSet(it.value.toLong())
                } else {
                    antallKandidatlisterTilknyttetDirektemeldtStillingPerMåned[it.key] = prometheusMeterRegistry.gauge(
                        "antall_kandidatlister_tilknyttet_direktemeldt_stilling_per_maaned",
                        Tags.of("maaned", it.key),
                        AtomicLong(it.value.toLong())
                    ) as AtomicLong
                }
            }
        }
    }
}