package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt


class HullICvTilDatakatalogStatistikk(private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
                                      private val dagensDato: () -> LocalDate): Runnable {

    companion object {
        private val filnavnAntallHull: String = "antallhull.json"
        private val filnavnAndelHull: String = "andelhull.json"
        private val fraDato = LocalDate.of(2021, 4, 8)
    }

    override fun run() {
        datakatalogKlient.sendPlotlyFilTilDatavarehus(
            filnavnAntallHull to lagPlot().toJsonString(),
            filnavnAndelHull to lagPlotAndelHull().toJsonString())
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
                    url = filnavnAntallHull
                )
            ),
            View(
                title = "Andel hull i cv",
                description = "Vise andel hull i cv",
                specType = "plotly",
                spec = Spec(
                    url = filnavnAndelHull
                )
            )
        )
    )



    private fun lagPlot() = Plotly.plot {
        fun Plot.lagBarAntallHull(hentVerdi: (Boolean?, LocalDate)-> Int, harHull: Boolean?, description: String) = bar {
            val datoer = dagerMellom(fraDato, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentVerdi(harHull, it) }
            name = description
        }
        val hullDatakatalog = repository.hentHullDatagrunnlag(dagerMellom(fraDato, dagensDato()))

        log.info("Henter data for hull for datakatalog")
        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, true, "Antall presentert med hull")
        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, false, "Antall presentert uten hull")
        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, null, "Antall presentert ukjent om de har hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, true, "Antall fått jobben med hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, false, "Antall fått jobben uten hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, null, "Antall fått jobben ukjent om de har hull")
        getLayout()
    }

    private fun lagPlotAndelHull() = Plotly.plot {
        fun Plot.lagBarAndelHull(hentVerdi: (LocalDate)-> Double, description: String) = bar {
            val datoer = dagerMellom(fraDato, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { (hentVerdi(it)*100).roundToInt() }
            name = description
        }
        val hullDatakatalog = repository.hentHullDatagrunnlag(dagerMellom(fraDato, dagensDato()))
        log.info("Henter data for hull for datakatalog")

        lagBarAndelHull(hullDatakatalog::hentAndelPresentert, "Andel presentert med hull")
        lagBarAndelHull(hullDatakatalog::hentAndelFåttJobben, "Andel fått jobben med hull")
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

private fun dagerMellom(fraDato: LocalDate, tilDato: LocalDate) = Period.between(fraDato, tilDato).days
    .let { antallDager ->
        (0..antallDager).map { fraDato + Period.ofDays(it) }
}