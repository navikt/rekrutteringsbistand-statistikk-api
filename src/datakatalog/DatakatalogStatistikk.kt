package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.Plot
import kscience.plotly.layout
import no.nav.rekrutteringsbistand.statistikk.datakatalog.alder.AlderStatistikk
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullStatistikk
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate
import java.time.Period
import no.nav.rekrutteringsbistand.statistikk.log


class DatakatalogStatistikk(
    private val kandidatutfallRepository: KandidatutfallRepository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {
    override fun run() {
        log.info("Starter jobb som sender statistikk til datakatalogen")
        plotlydataOgDataPakke().also { (plotly, datapakke) ->
            datakatalogKlient.sendPlotlyFilTilDatavarehus(plotly)
            datakatalogKlient.sendDatapakke(datapakke)
        }
        log.info("Har gjennomført jobb som sender statistikk til datakatalogen")
    }

    private fun datapakke(views: List<View>) =
        Datapakke(
            title = "Rekrutteringsbistand statistikk",
            description = "Vise rekrutteringsbistand statistikk",
            resources = emptyList(),
            views = views
        )

    private fun plotlydataOgDataPakke() = listOf(
        HullStatistikk(kandidatutfallRepository, dagensDato),
        AlderStatistikk(kandidatutfallRepository, dagensDato)
    ).let {
        it.flatMap(DatakatalogData::plotlyFiler) to it.flatMap(DatakatalogData::views).let(this::datapakke)
    }
}

fun Plot.getLayout(yTekst: String) {
    layout {
        bargap = 0.1
        title {
            text = ""
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
