package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.time.Period


class HullICvTilDatakatalogStatistikk(private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
                                      private val dagensDato: () -> LocalDate): Runnable {

    companion object {
        internal val filnavnAntallHull: String = "antallhull.json"
        internal val filnavnAndelHull: String = "andelhull.json"
         internal val fraDato = LocalDate.of(2021, 4, 8)
    }

    override fun run() {
        datakatalogKlient.sendPlotlyFilTilDatavarehus(lagPlotAntallHull().toJsonString(), lagPlotAndelHull().toJsonString())
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

    private fun lagPlotAntallHull() = Plotly.plot {
        fun Plot.lagBar(hentAntall: (Boolean?, LocalDate, LocalDate)-> Int, harHull: Boolean?, description: String) = bar {
            val datoer = dagerMellom(fraDato, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentAntall(harHull, it, it.plusDays(1)) }
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

    private fun lagPlotAndelHull() = Plotly.plot {
        fun Plot.lagBar(hentAntall: (Boolean?, LocalDate, LocalDate)-> Int, harHull: Boolean?, description: String) = bar {
            val datoer = dagerMellom(fraDato, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentAntall(harHull, it, it.plusDays(1)) }
            name = description
        }

        log.info("Henter data for hull for datakatalog")
        lagBar(repository::hentAntallPresentert, true, "Andel presentert med hull")
        lagBar(repository::hentAndelFåttJobben, false, "Andel fått jobben med hull")
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