package statistikkapi.datakatalog.tilretteleggingsbehov

import kscience.plotly.Plotly
import kscience.plotly.toJsonString
import statistikkapi.datakatalog.*

class TilretteleggingsbehovStatistikk(private val tilretteleggingsbehovDatagrunnlag: TilretteleggingsbehovDatagrunnlag): DatakatalogData {
    companion object {
        private val filnavnTilretteleggingsbehovAntallPresentert: String = "tilretteleggingsbehovAntallPresentert.json"
        private val filnavnTilretteleggingsbehovAndelPresentert: String = "tilretteleggingsbehovAndelPresentert.json"
        private val filnavnTilretteleggingsbehovAntallFåttJobben: String = "tilretteleggingsbehovAntallFåttJobben.json"
        private val filnavnTilretteleggingsbehovAndelFåttJobben: String = "tilretteleggingsbehovAndelFåttJobben.json"
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
        tilretteleggingsbehovDatagrunnlag.let { datagrunnlag ->
            listOf(
                filnavnTilretteleggingsbehovAntallPresentert to lagPlotAntallTilretteleggingsbehovPresentert(datagrunnlag).toJsonString(),
                filnavnTilretteleggingsbehovAntallFåttJobben to lagPlotAntallTilretteleggingsbehovFåttJobben(datagrunnlag).toJsonString(),
                filnavnTilretteleggingsbehovAndelPresentert to lagPlotTilretteleggingsbehovAndelPresentert(datagrunnlag).toJsonString(),
                filnavnTilretteleggingsbehovAndelFåttJobben to lagPlotTilretteleggingsbehovAndelFåttJobben(datagrunnlag).toJsonString()
            )
        }

    private fun lagPlotAntallTilretteleggingsbehovPresentert(datagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        datagrunnlag.listeAvBehov().forEach { behov ->
            lagBar("Antall presentert med tilretteleggingsbehov $behov", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(behov, it) }
        }
        getLayout("Antall")
    }

    private fun lagPlotAntallTilretteleggingsbehovFåttJobben(datagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        datagrunnlag.listeAvBehov().forEach { behov: String ->
            lagBar("Antall fått jobben med tilretteleggingsbehov $behov", datagrunnlag.gjeldendeDatoer()) {datagrunnlag.hentAntallFåttJobben(behov, it)}
        }
        getLayout("Antall")
    }

    private fun lagPlotTilretteleggingsbehovAndelPresentert(datagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        lagBar("Andel presentert med minst et tilretteleggingsbehov", datagrunnlag.gjeldendeDatoer()) {datagrunnlag.hentAndelPresentertMedMinstEttTilretteleggingsbehov(it).somProsent()}
        getLayout("Andel %")
    }

    private fun lagPlotTilretteleggingsbehovAndelFåttJobben(datagrunnlag: TilretteleggingsbehovDatagrunnlag) = Plotly.plot {
        lagBar("Andel fått jobben med minst et tilretteleggingsbehov", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelFåttJobbenmedMinstEttTilretteleggingsbehov(it).somProsent() }
        getLayout("Andel %")
    }
}
