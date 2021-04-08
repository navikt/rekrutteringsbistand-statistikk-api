package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate

fun sendHullICvTilDatakatalog(repository: Repository) = Runnable {
    log.info("Henter data for hull for katakatalog")
    val iDag = LocalDate.now()
    val fåttJobbenMedHull = repository.hentAntallFåttJobben(harHull = true, iDag.minusYears(5), iDag)
    val fåttJobbenUtenHull = repository.hentAntallFåttJobben(harHull = true, iDag.minusYears(5), iDag)
    val fåttJobbenUkjentHull = repository.hentAntallFåttJobben(harHull = null, iDag.minusYears(5), iDag)


    val presentertMedHull = repository.hentAntallPresentert(harHull = true, iDag.minusYears(5), iDag)
    val presentertUtenHull = repository.hentAntallPresentert(harHull = true, iDag.minusYears(5), iDag)
    val presentertUkjentHull = repository.hentAntallPresentert(harHull = null, iDag.minusYears(5), iDag)

    log.info("Har hentet data for hull for datakatalog: " +
            "fåttJobbenMedHull: $fåttJobbenMedHull, " +
            "fåttJobbenUtenHull: $fåttJobbenUtenHull, " +
            "fåttJobbenUkjentHull: $fåttJobbenUkjentHull, " +
            "presentertMedHull: $presentertMedHull, " +
            "presentertUtenHull: $presentertUtenHull, " +
            "presentertUkjentHull: $presentertUkjentHull")

    return@Runnable
}