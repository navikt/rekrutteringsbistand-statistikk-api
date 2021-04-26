package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.Plot
import kscience.plotly.layout
import no.nav.rekrutteringsbistand.statistikk.datakatalog.alder.AlderStatistikk
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.DatakatalogData
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import java.time.LocalDate
import java.time.Period


class DatakatalogStatistikk(
    private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {

    override fun run() {

        val (views, plotlydata) = listOf(
            HullStatistikk(repository, dagensDato),
            AlderStatistikk(repository, dagensDato)
        ).run {
            flatMap(DatakatalogData::views) to flatMap(DatakatalogData::plotlyFiler)
        }

        val datapakke = Datapakke(
            title = "Rekrutteringsbistand statistikk",
            description = "Vise rekrutteringsbistand statistikk",
            resources = emptyList(),
            views = views
        )

        datakatalogKlient.sendPlotlyFilTilDatavarehus(plotlydata)
        datakatalogKlient.sendDatapakke(datapakke)
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