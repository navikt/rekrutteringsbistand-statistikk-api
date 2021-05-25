package statistikkapi.datakatalog

import kscience.plotly.Plot
import kscience.plotly.bar
import kscience.plotly.layout
import statistikkapi.log
import statistikkapi.datakatalog.alder.AlderDatagrunnlag
import statistikkapi.datakatalog.alder.AlderStatistikk
import statistikkapi.datakatalog.hull.HullDatagrunnlag
import statistikkapi.datakatalog.hull.HullStatistikk
import statistikkapi.datakatalog.tilretteleggingsbehov.TilretteleggingsbehovDatagrunnlag
import statistikkapi.datakatalog.tilretteleggingsbehov.TilretteleggingsbehovStatistikk
import statistikkapi.kandidatutfall.KandidatutfallRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


class DatakatalogStatistikk(
    private val kandidatutfallRepository: KandidatutfallRepository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {

    private val målingerStartet = LocalDate.of(2021, 4, 8)

    override fun run() {
        log.info("Starter jobb som sender statistikk til datakatalogen")
        log.info("Skal sende statistikk for målinger til og med ${dagensDato}")
        try {
            plotlydataOgDataPakke().also { (plotly, datapakke) ->
                datakatalogKlient.sendPlotlyFilTilDatavarehus(plotly)
                datakatalogKlient.sendDatapakke(datapakke)
            }
        } catch (e: Exception) {
            log.warn("Feil ved sending av datapakke til datavarehus.")
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

    private fun plotlydataOgDataPakke() = (
            kandidatutfallRepository.hentUtfallPresentert(målingerStartet) to
                    kandidatutfallRepository.hentUtfallFåttJobben(målingerStartet))
        .let { (utfallElementPresentert, utfallElementFåttJobben) ->
            listOf(
                HullStatistikk(HullDatagrunnlag(utfallElementPresentert,utfallElementFåttJobben,dagensDato)),
                AlderStatistikk(AlderDatagrunnlag(utfallElementPresentert,utfallElementFåttJobben,dagensDato)),
                TilretteleggingsbehovStatistikk(TilretteleggingsbehovDatagrunnlag(utfallElementPresentert,utfallElementFåttJobben,dagensDato))
            ).let {
                it.flatMap(DatakatalogData::plotlyFiler) to it.flatMap(DatakatalogData::views).let(this::datapakke)
            }
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

fun Plot.lagBar(description: String, datoer: List<LocalDate>, hentVerdi: (LocalDate) -> Int) = bar {
    x.strings = datoer.map { it.toString() }
    y.numbers = datoer.map { hentVerdi(it) }
    name = description
}

fun Double.somProsent() = (this * 100).roundToInt()


infix fun LocalDate.til(tilDato: LocalDate) = ChronoUnit.DAYS.between(this, tilDato)
    .let { antallDager ->
        (0..antallDager).map { this.plusDays(it) }
    }
