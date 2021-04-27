package no.nav.rekrutteringsbistand.statistikk.datakatalog.hull

import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.toJsonString
import no.nav.rekrutteringsbistand.statistikk.datakatalog.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import kotlin.math.roundToInt

class HullStatistikk(private val repository: Repository, private val dagensDato: () -> LocalDate) : DatakatalogData {
    companion object {
        private val filnavnHullAntallPresentert: String = "hullAntallPresentert.json"
        private val filnavnHullAndelPresentert: String = "hullAndelPresentert.json"
        private val filnavnHullAntallFåttJobben: String = "hullAntallFåttJobben.json"
        private val filnavnHullAndelFåttJobben: String = "hullAndelFåttJobben.json"
        private val fraDatoHull = LocalDate.of(2021, 4, 8)
    }


    override fun views() = listOf(
        View(
            title = "Antall hull presentert",
            description = "Vise antall hull presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullAntallPresentert
            )
        ),
        View(
            title = "Andel hull presentert",
            description = "Vise andel hull presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullAndelPresentert
            )
        ),

        View(
            title = "Antall hull fått jobben",
            description = "Vise antall fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullAntallFåttJobben
            )
        ),
        View(
            title = "Andel hull fått jobben",
            description = "Vise andel hull fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullAndelFåttJobben
            )
        )

    )

    override fun plotlyFiler() =
        repository.hentHullDatagrunnlag(dagerMellom(fraDatoHull, dagensDato())).let { hullDatakatalog ->
            listOf(
                filnavnHullAntallPresentert to lagPlotAntallHullPresentert(hullDatakatalog).toJsonString(),
                filnavnHullAntallFåttJobben to lagPlotAntallHullFåttJobben(hullDatakatalog).toJsonString(),
                filnavnHullAndelPresentert to lagPlotHullAndelPresentert(hullDatakatalog).toJsonString(),
                filnavnHullAndelFåttJobben to lagPlotHullAndelFåttJobben(hullDatakatalog).toJsonString()
            )
        }

    private fun Plot.lagBarAntallHull(hentVerdi: (Boolean?, LocalDate) -> Int, harHull: Boolean?, description: String) =
        bar {
            val datoer = dagerMellom(fraDatoHull, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentVerdi(harHull, it) }
            name = description
        }

    private fun lagPlotAntallHullPresentert(hullDatagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBarAntallHull(hullDatagrunnlag::hentAntallPresentert, true, "Antall presentert med hull")
        lagBarAntallHull(hullDatagrunnlag::hentAntallPresentert, false, "Antall presentert uten hull")
        lagBarAntallHull(hullDatagrunnlag::hentAntallPresentert, null, "Antall presentert ukjent om de har hull")
        getLayout("Antall")
    }

    private fun lagPlotAntallHullFåttJobben(hullDatagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBarAntallHull(hullDatagrunnlag::hentAntallFåttJobben, true, "Antall fått jobben med hull")
        lagBarAntallHull(hullDatagrunnlag::hentAntallFåttJobben, false, "Antall fått jobben uten hull")
        lagBarAntallHull(hullDatagrunnlag::hentAntallFåttJobben, null, "Antall fått jobben ukjent om de har hull")
        getLayout("Antall")
    }

    private fun Plot.lagBarAndelHull(hentVerdi: (LocalDate) -> Double, description: String) = bar {
        val datoer = dagerMellom(fraDatoHull, dagensDato())
        x.strings = datoer.map { it.toString() }
        y.numbers = datoer.map { (hentVerdi(it) * 100).roundToInt() }
        name = description
    }

    private fun lagPlotHullAndelPresentert(hullDatagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBarAndelHull(hullDatagrunnlag::hentAndelPresentert, "Andel presentert med hull")
        getLayout("Andel %")
    }

    private fun lagPlotHullAndelFåttJobben(hullDatagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBarAndelHull(hullDatagrunnlag::hentAndelFåttJobben, "Andel fått jobben med hull")
        getLayout("Andel %")
    }
}