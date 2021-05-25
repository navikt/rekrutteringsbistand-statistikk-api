package statistikkapi.datakatalog.alder

import kscience.plotly.Plotly
import kscience.plotly.toJsonString
import statistikkapi.datakatalog.*

class AlderStatistikk(private val alderDatagrunnlag: AlderDatagrunnlag) : DatakatalogData {

    companion object {
        private val filnavnAlderAntallPresentert: String = "alderAntallPresentert.json"
        private val filnavnAlderAndelPresentert: String = "alderAndelPresentert.json"
        private val filnavnAlderAntallFåttJobben: String = "alderAntallFåttJobben.json"
        private val filnavnAlderAndelFåttJobben: String = "alderAndelFåttJobben.json"
    }

    override fun views() = listOf(
        View(
            title = "Antall alder presentert",
            description = "Vise antall alder presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnAlderAntallPresentert
            )
        ),
        View(
            title = "Andel alder presentert",
            description = "Vise andel alder presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnAlderAndelPresentert
            )
        ),
        View(
            title = "Antall alder fått jobben",
            description = "Vise antall alder fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnAlderAntallFåttJobben
            )
        ),
        View(
            title = "Andel alder fått jobben",
            description = "Vise andel alder fått jobben",
            specType = "plotly",
            spec = Spec(
                url = filnavnAlderAndelFåttJobben
            )
        )
    )

    override fun plotlyFiler() =
        alderDatagrunnlag.let { datagrunnlag ->
            listOf(
                filnavnAlderAntallPresentert to lagPlotAlderPresentert(datagrunnlag).toJsonString(),
                filnavnAlderAntallFåttJobben to lagPlotAlderFåttJobben(datagrunnlag).toJsonString(),
                filnavnAlderAndelPresentert to lagPlotAlderAndelPresentert(datagrunnlag).toJsonString(),
                filnavnAlderAndelFåttJobben to lagPlotAlderAndelFåttJobben(datagrunnlag).toJsonString()
            )
        }

    private fun lagPlotAlderPresentert(datagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        lagBar("Antall presentert under 30", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(Aldersgruppe.under30, it) }
        lagBar("Antall presentert over 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(Aldersgruppe.over50, it) }
        lagBar("Antall presentert mellom 30 og 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(Aldersgruppe.mellom30og50, it) }
        getLayout("Antall")
    }

    private fun lagPlotAlderAndelPresentert(datagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        lagBar("Andel presentert under 30", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelPresentertUng(it).somProsent() }
        lagBar("Andel presentert over 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelPresentertSenior(it).somProsent() }
        getLayout("Andel %")
    }

    private fun lagPlotAlderFåttJobben(datagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        lagBar("Antall fått jobben under 30", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(Aldersgruppe.under30, it) }
        lagBar("Antall fått jobben over 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(Aldersgruppe.over50, it) }
        lagBar("Antall fått jobben mellom 30 og 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(Aldersgruppe.mellom30og50, it) }
        getLayout("Antall")
    }

    private fun lagPlotAlderAndelFåttJobben(datagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        lagBar("Andel fått jobben under 30", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelFåttJobbenUng(it).somProsent() }
        lagBar("Andel fått jobben over 50", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelFåttJobbenSenior(it).somProsent() }
        getLayout("Andel %")
    }
}