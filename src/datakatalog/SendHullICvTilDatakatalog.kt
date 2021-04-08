package no.nav.rekrutteringsbistand.statistikk.datakatalog

import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.util.*


fun sendHullICvTilDatakatalog(repository: Repository) = Runnable {
    log.info("Henter data for hull for katakatalog")
    val fraDato = LocalDate.of(2021, 4, 6)
    val datoer: List<LocalDate> = finnDagerFra(fraDato)

    log.info("Henter data for hull for katakatalog for dager: $datoer")

    datoer.map {
        val fåttJobbenMedHull = repository.hentAntallFåttJobben(harHull = true, it, it)
        val fåttJobbenUtenHull = repository.hentAntallFåttJobben(harHull = false, it, it)
        val fåttJobbenUkjentHull = repository.hentAntallFåttJobben(harHull = null, it, it)

        val presentertMedHull = repository.hentAntallPresentert(harHull = true, it, it)
        val presentertUtenHull = repository.hentAntallPresentert(harHull = false, it, it)
        val presentertUkjentHull = repository.hentAntallPresentert(harHull = null, it, it)



        log.info(
            "Har hentet data for hull for datakatalog for $it: " +
                    "fåttJobbenMedHull: $fåttJobbenMedHull, " +
                    "fåttJobbenUtenHull: $fåttJobbenUtenHull, " +
                    "fåttJobbenUkjentHull: $fåttJobbenUkjentHull, " +
                    "presentertMedHull: $presentertMedHull, " +
                    "presentertUtenHull: $presentertUtenHull, " +
                    "presentertUkjentHull: $presentertUkjentHull"
        )
    }



    return@Runnable
}

private tailrec fun finnDagerFra(dato: LocalDate): List<LocalDate> =
    if (dato.isAfter(LocalDate.now())) emptyList()
    else listOf(dato) + finnDagerFra(dato.plusDays(1))
