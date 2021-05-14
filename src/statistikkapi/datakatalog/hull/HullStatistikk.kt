package statistikkapi.datakatalog.hull

import kscience.plotly.Plotly
import kscience.plotly.toJsonString
import statistikkapi.datakatalog.*
import statistikkapi.log

class HullStatistikk(private val datagrunnlag: HullDatagrunnlag) : DatakatalogData {
    companion object {
        private val filnavnHullAntallPresentert: String = "hullAntallPresentert.json"
        private val filnavnHullAndelPresentert: String = "hullAndelPresentert.json"
        private val filnavnHullAntallFåttJobben: String = "hullAntallFåttJobben.json"
        private val filnavnHullAndelFåttJobben: String = "hullAndelFåttJobben.json"
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
        datagrunnlag.let { datagrunnlag ->
            listOf(
                filnavnHullAntallPresentert to lagPlotAntallHullPresentert(datagrunnlag).toJsonString(),
                filnavnHullAntallFåttJobben to lagPlotAntallHullFåttJobben(datagrunnlag).toJsonString(),
                filnavnHullAndelPresentert to lagPlotHullAndelPresentert(datagrunnlag).toJsonString(),
                filnavnHullAndelFåttJobben to lagPlotHullAndelFåttJobben(datagrunnlag).toJsonString()
            )
        }

    private fun lagPlotAntallHullPresentert(datagrunnlag: HullDatagrunnlag) = Plotly.plot {
        log.info("Skal lage diagram for antall hull presentert i perioden ${datagrunnlag.gjeldendeDatoer().first()} - ${datagrunnlag.gjeldendeDatoer().last()}")
        lagBar("Antall presentert med hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(true, it) }
        lagBar("Antall presentert uten hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(false, it) }
        lagBar("Antall presentert ukjent om de har hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallPresentert(null, it) }
        getLayout("Antall")
    }

    private fun lagPlotAntallHullFåttJobben(datagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBar("Antall fått jobben med hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(true, it) }
        lagBar("Antall fått jobben uten hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(false, it) }
        lagBar("Antall fått jobben ukjent om de har hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAntallFåttJobben(null, it) }
        getLayout("Antall")
    }

    private fun lagPlotHullAndelPresentert(datagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBar("Andel presentert med hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelPresentert(it).somProsent() }
        getLayout("Andel %")
    }

    private fun lagPlotHullAndelFåttJobben(datagrunnlag: HullDatagrunnlag) = Plotly.plot {
        lagBar("Andel fått jobben med hull", datagrunnlag.gjeldendeDatoer()) { datagrunnlag.hentAndelPresentert(it).somProsent() }
        getLayout("Andel %")
    }
}
