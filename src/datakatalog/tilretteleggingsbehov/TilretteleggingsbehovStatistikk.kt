package no.nav.rekrutteringsbistand.statistikk.datakatalog.tilretteleggingsbehov

import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.toJsonString
import no.nav.rekrutteringsbistand.statistikk.datakatalog.*
import java.time.LocalDate
import kotlin.math.roundToInt

class TilretteleggingsbehovStatistikk(private val datagrunnlag: Datagrunnlag, private val dagensDato: () -> LocalDate): DatakatalogData {
    companion object {
        private val filnavnTilretteleggingsbehovAntallPresentert: String = "tilretteleggingsbehovAntallPresentert.json"
        private val filnavnTilretteleggingsbehovAndelPresentert: String = "tilretteleggingsbehovAndelPresentert.json"
        private val filnavnTilretteleggingsbehovAntallFåttJobben: String = "tilretteleggingsbehovAntallFåttJobben.json"
        private val filnavnTilretteleggingsbehovAndelFåttJobben: String = "tilretteleggingsbehovAndelFåttJobben.json"
        private val fraDatoTilrettelegingsbehov = LocalDate.of(2021, 5, 4)
    }


    override fun views() = listOf(
        View(
            title = "Antall med forskjellige tilretteleggingsbehov presentert",
            description = "Vise antall med forskjellige tilretteleggingsbehov presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnTilretteleggingsbehovAntallPresentert
            )
        ),
        View(
            title = "Andel med minst et tilretteleggingsbehov presentert",
            description = "Vise andel med minst et tilretteleggingsbehov presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnTilretteleggingsbehovAndelPresentert
            )
        ),

        View(
            title = "Antall med forskjellige tilretteleggingsbehov som har fått jobben",
            description = "Vise antall med forskjellige tilretteleggingsbehov som har fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnTilretteleggingsbehovAntallFåttJobben
            )
        ),
        View(
            title = "Andel med minst et tilretteleggingsbehov som har fått jobben",
            description = "Vise andel med minst et tilretteleggingsbehov som har fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnTilretteleggingsbehovAndelFåttJobben
            )
        )

    )

    override fun plotlyFiler() =
        datagrunnlag.hentTilretteleggingsbehovDatagrunnlag(fraDatoTilrettelegingsbehov til dagensDato()).let { tilretteleggingsbehovDatakatalog ->
            listOf(
                filnavnTilretteleggingsbehovAntallPresentert to lagPlotAntallTilretteleggingsbehovPresentert(tilretteleggingsbehovDatakatalog).toJsonString(),
                filnavnTilretteleggingsbehovAntallFåttJobben to lagPlotAntallTilretteleggingsbehovFåttJobben(tilretteleggingsbehovDatakatalog).toJsonString(),
                filnavnTilretteleggingsbehovAndelPresentert to lagPlotTilretteleggingsbehovAndelPresentert(tilretteleggingsbehovDatakatalog).toJsonString(),
                filnavnTilretteleggingsbehovAndelFåttJobben to lagPlotTilretteleggingsbehovAndelFåttJobben(tilretteleggingsbehovDatakatalog).toJsonString()
            )
        }

    private fun Plot.lagBarAntallTilretteleggingsbehov(hentVerdi: (String, LocalDate) -> Int, tilretteleggingsbehov: String, description: String) =
        bar {
            val datoer = fraDatoTilrettelegingsbehov til dagensDato()
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentVerdi(tilretteleggingsbehov, it) }
            name = description
        }

    private fun lagPlotAntallTilretteleggingsbehovPresentert(tilretteleggingsbehovDatagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        tilretteleggingsbehovDatagrunnlag.listeAvBehov().forEach {
            lagBarAntallTilretteleggingsbehov(tilretteleggingsbehovDatagrunnlag::hentAntallPresentert, it, "Antall presentert med tilretteleggingsbehov $it")
        }
        getLayout("Antall")
    }


    private fun lagPlotAntallTilretteleggingsbehovFåttJobben(tilretteleggingsbehovDatagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        tilretteleggingsbehovDatagrunnlag.listeAvBehov().forEach {
            lagBarAntallTilretteleggingsbehov(tilretteleggingsbehovDatagrunnlag::hentAntallFåttJobben, it, "Antall fått jobben med tilretteleggingsbehov $it")
        }
        getLayout("Antall")
    }

    private fun Plot.lagBarAndelHull(hentVerdi: (LocalDate) -> Double, description: String) = bar {
        val datoer = fraDatoTilrettelegingsbehov til dagensDato()
        x.strings = datoer.map { it.toString() }
        y.numbers = datoer.map { (hentVerdi(it) * 100).roundToInt() }
        name = description
    }

    private fun lagPlotTilretteleggingsbehovAndelPresentert(tilretteleggingsbehovDatagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        lagBarAndelHull(tilretteleggingsbehovDatagrunnlag::hentAndelPresentert, "Andel presentert med minst et tilretteleggingsbehov")
        getLayout("Andel %")
    }

    private fun lagPlotTilretteleggingsbehovAndelFåttJobben(tilretteleggingsbehovDatagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        lagBarAndelHull(tilretteleggingsbehovDatagrunnlag::hentAndelFåttJobben, "Andel fått jobben med minst et tilretteleggingsbehov")
        getLayout("Andel %")
    }
}
