package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.Plot
import kscience.plotly.layout
import no.nav.rekrutteringsbistand.statistikk.datakatalog.alder.AlderStatistikk
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import java.time.LocalDate
import java.time.Period


class DatakatalogStatistikk(
    private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {
    override fun run() {
        plotlydataOgDataPakke().also { (plotly, datapakke) ->
            datakatalogKlient.sendPlotlyFilTilDatavarehus(plotly)
            datakatalogKlient.sendDatapakke(datapakke)
        }
    }

    private fun datapakke(views: List<View>) =
        Datapakke(
            title = "Rekrutteringsbistand statistikk",
            description = "Vise rekrutteringsbistand statistikk",
            resources = emptyList(),
            views = views
        )

    private fun plotlydataOgDataPakke() = listOf(
        HullStatistikk(repository, dagensDato),
        AlderStatistikk(repository, dagensDato)
    ).let {
        it.flatMap(DatakatalogData::plotlyFiler) to it.flatMap(DatakatalogData::views).let(this::datapakke)
    }
}

fun Plot.getLayout(yTekst: String) {
    layout {
        bargap = 0.1
        title {
            text = "Basic Histogram"
            font {
                size = 20
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
                text = yTekst
                font {
                    size = 16
                }
            }
        }
    }
}

fun dagerMellom(fraDato: LocalDate, tilDato: LocalDate) = Period.between(fraDato, tilDato).days
    .let { antallDager ->
        (0..antallDager).map { fraDato + Period.ofDays(it) }
    }