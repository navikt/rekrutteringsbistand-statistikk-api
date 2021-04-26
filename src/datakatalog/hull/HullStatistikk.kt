package no.nav.rekrutteringsbistand.statistikk.datakatalog.hull

import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.toJsonString
import no.nav.rekrutteringsbistand.statistikk.datakatalog.Spec
import no.nav.rekrutteringsbistand.statistikk.datakatalog.View
import no.nav.rekrutteringsbistand.statistikk.datakatalog.dagerMellom
import no.nav.rekrutteringsbistand.statistikk.datakatalog.getLayout
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import kotlin.math.roundToInt

class HullStatistikk (private val repository: Repository, private val dagensDato: () -> LocalDate) {
    companion object {
        private val filnavnHullPresentert: String = "hullPresentert.json"
        private val filnavnHullFåttJobben: String = "hullFåttJobben.json"
        private val filnavnHullAndelPresentert: String = "hullAndelPresentert.json"
        private val filnavnHullAndelFåttJobben: String = "hullAndelFåttJobben.json"
        private val fraDatoHull = LocalDate.of(2021, 4, 8)
    }

    fun views() = listOf(
        View(
            title = "Antall hull presentert",
            description = "Vise antall hull presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullPresentert
            )
        ),
        View(
            title = "Antall hull fått jobben",
            description = "Vise antall fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullFåttJobben
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
            title = "Andel hull fått jobben",
            description = "Vise andel hull fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnHullAndelFåttJobben
            )
        )

    )

    fun plotlyFiler() = repository.hentHullDatagrunnlag(dagerMellom(fraDatoHull, dagensDato())).let {hullDatakatalog ->
        listOf(
            filnavnHullPresentert to lagPlotAntallHullPresentert(hullDatakatalog).toJsonString(),
            filnavnHullFåttJobben to lagPlotAntallHullFåttJobben(hullDatakatalog).toJsonString(),
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
        log.info("Henter data for hull for datakatalog")

        lagBarAndelHull(hullDatagrunnlag::hentAndelPresentert, "Andel presentert med hull")
        getLayout("Andel %")
    }

    private fun lagPlotHullAndelFåttJobben(hullDatagrunnlag: HullDatagrunnlag) = Plotly.plot {
        log.info("Henter data for hull for datakatalog")

        lagBarAndelHull(hullDatagrunnlag::hentAndelFåttJobben, "Andel fått jobben med hull")
        getLayout("Andel %")
    }
}