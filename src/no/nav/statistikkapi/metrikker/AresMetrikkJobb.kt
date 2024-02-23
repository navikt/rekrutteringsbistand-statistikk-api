package no.nav.statistikkapi.metrikker

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.logging.log
import no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepository
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MINUTES

class AresMetrikkJobb(
    private val kandidatutfallRepository: KandidatutfallRepository,
    private val kandidatlisteRepository: KandidatlisteRepository,
    private val visningKontaktinfoRepository: VisningKontaktinfoRepository,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        executor.scheduleWithFixedDelay(this::lesFraDbOgSkrivTilPrometheus, 1, 5, MINUTES)
    }

    private fun lesFraDbOgSkrivTilPrometheus() {
        try {
            lesOgSkriv()
        } catch (e: Exception) {
            log.error("Forsøkte å lese statistikk fra DB og skrive metrikk til Prometheus", e)
        }

    }

    private fun lesOgSkriv() {
        prometheusMeterRegistry.gauge(
            "antall_presenterte_kandidater",
            kandidatutfallRepository.hentAntallPresentertForAlleNavKontor()
        )

        prometheusMeterRegistry.gauge(
            "antall_fått_jobben",
            kandidatutfallRepository.hentAntallFåttJobbenForAlleNavKontor()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_tilknyttet_stilling",
            kandidatlisteRepository.hentAntallKandidatlisterForOpprettedeStillinger()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_tilknyttet_direktemeldt_stilling",
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldteStillinger()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_tilknyttet_ekstern_stilling",
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetEksterneStillinger()
        )

        prometheusMeterRegistry.gauge(
            "antall_stillinger_for_eksterne_stillingsannonser_med_kandidatliste",
            kandidatlisteRepository.hentAntallStillingerForEksterneStillingsannonserMedKandidatliste()
        )

        prometheusMeterRegistry.gauge(
            "antall_stillinger_for_direktemeldte_stillingsannonser",
            kandidatlisteRepository.hentAntallStillingerForDirektemeldteStillingsannonser()
        )

        prometheusMeterRegistry.gauge(
            "antall_stillinger_for_stillingsannonser_med_kandidatliste",
            kandidatlisteRepository.hentAntallStillingerForStillingsannonserMedKandidatliste()
        )

        prometheusMeterRegistry.gauge(
            "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat",
            kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidater_i_prioritert_malgruppe_som_har_fatt_vist_sin_kontaktinfo",
            visningKontaktinfoRepository.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo",
            visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben",
            kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobben()
        )

        prometheusMeterRegistry.gauge(
            "antall_direktemeldte_stillinger_som_har_tom_kandidatliste",
            kandidatlisteRepository.hentAntallDirektemeldteStillingerSomHarTomKandidatliste()
        )

        prometheusMeterRegistry.gauge(
            "antall_unike_arbeidsgivere_for_direktemeldte_stillinger",
            kandidatlisteRepository.hentAntallUnikeArbeidsgivereForDirektemeldteStillinger()
        )

        prometheusMeterRegistry.gauge(
            "antall_kandidatlister_tilknyttet_direktemeldt_stilling_der_minst_en_kandidat_fikk_jobben",
            kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingDerMinstEnKandidatFikkJobben()
        )

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetStillingPerMåned().forEach {
            prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_stilling_per_maaned",
                Tags.of("maaned", it.key),
                it.value
            )
        }

        kandidatlisteRepository.hentAntallKandidatlisterTilknyttetDirektemeldtStillingPerMåned().forEach {
            prometheusMeterRegistry.gauge(
                "antall_kandidatlister_tilknyttet_direktemeldt_stilling_per_maaned",
                Tags.of("maaned", it.key),
                it.value
            )
        }

        kandidatlisteRepository.hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidatPerMåned().forEach {
            prometheusMeterRegistry.gauge(
                "antall_direktemeldte_stillinger_med_minst_en_presentert_kandidat_per_maaned",
                Tags.of("maaned", it.key),
                it.value
            )
        }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatIPrioritertMålgruppeSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                prometheusMeterRegistry.gauge(
                    "antall_kandidatlister_med_minst_en_kandidat_i_prioritert_maalgruppe_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                    Tags.of("maaned", it.key),
                    it.value
                )
            }

        visningKontaktinfoRepository.hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()
            .forEach {
                prometheusMeterRegistry.gauge(
                    "antall_kandidatlister_med_minst_en_kandidat_som_har_faatt_vist_sin_kontaktinfo_per_maaned",
                    Tags.of("maaned", it.key),
                    it.value
                )
            }

        kandidatlisteRepository.hentAntallKandidatlisterDerMinstEnKandidatIPrioritertMålgruppeFikkJobbenPerMåned()
            .forEach {
                prometheusMeterRegistry.gauge(
                    "antall_kandidatlister_der_minst_en_kandidat_i_prioritert_maalgruppe_fikk_jobben_per_maaned",
                    Tags.of("maaned", it.key),
                    it.value
                )
            }
    }
}
