package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.datakatalog.alder.AlderStatistikk
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt


class DatakatalogStatistikk(
    private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {

    override fun run() {
        datakatalogKlient.sendPlotlyFilTilDatavarehus(
            listOf(
                HullStatistikk(repository, dagensDato).plotlyFiler(),
                AlderStatistikk(repository, dagensDato).plotlyFiler()
            ).flatten()
        )
        datakatalogKlient.sendDatapakke(lagDatapakke())
    }

    private fun lagDatapakke() = Datapakke(
        title = "Rekrutteringsbistand statistikk",
        description = "Vise rekrutteringsbistand statistikk",
        resources = emptyList(),
        views = listOf(
            HullStatistikk(repository, dagensDato).views(),
            AlderStatistikk(repository, dagensDato).views()
        ).flatten()
    )
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