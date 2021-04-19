package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.time.Period


class HullICvTilDatakatalogStatistikk(private val repository: Repository, private val datakatalogKlient: DatakatalogKlient): Runnable {

    companion object {
        private val filnavn: String = "antallhull.json"
        private val fraDato = LocalDate.of(2021, 4, 6)
    }

    override fun run() {
        datakatalogKlient.sendPlotlyFilTilDatavarehus(lagPlot().toJsonString())
        datakatalogKlient.sendDatapakke(lagDatapakke())
    }

    private fun lagDatapakke() = Datapakke(
        title = "Hull i cv",
        description = "Vise hull i cv",
        resources = emptyList(),
        views = listOf(
            View(
                title = "Antall hull i cv",
                description = "Vise antall hull i cv",
                specType = "plotly",
                spec = Spec(
                    url = filnavn
                )
            )
        )
    )

    private fun lagPlot() = Plotly.plot {
        fun Plot.lagBar(f: (Boolean?, LocalDate, LocalDate)-> Int, harHull: Boolean?, description: String) = bar {
            val datoer = dagerSidenRange(fraDato)
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { f(harHull, it, it.plusDays(1)) }
            name = description
        }

        log.info("Henter data for hull for datakatalog")
        lagBar(repository::hentAntallPresentert, true, "Antall presentert med hull")
        lagBar(repository::hentAntallPresentert, false, "Antall presentert uten hull")
        lagBar(repository::hentAntallPresentert, null, "Antall presentert ukjent om de har hull")
        lagBar(repository::hentAntallFåttJobben, true, "Antall fått jobben med hull")
        lagBar(repository::hentAntallFåttJobben, false, "Antall fått jobben uten hull")
        lagBar(repository::hentAntallFåttJobben, null, "Antall fått jobben ukjent om de har hull")
        getLayout()
    }
}


private fun Plot.getLayout() {
    layout {
        bargap = 0.1
        title {
            text = "Basic Histogram"
            font {
                size = 20
                color("black")
            }
        }
        xaxis {
            title {
                text = "Dato"
                font {
                    size = 16
                }
            }
        }
        yaxis {
            title {
                text = "Antall"
                font {
                    size = 16
                }
            }
        }
    }
}

private fun dagerSidenRange(fraDato: LocalDate) = Period.between(fraDato, LocalDate.now()).days
    .let { antallDager ->
        (0..antallDager).map { LocalDate.now() - Period.ofDays(antallDager + it) }
}