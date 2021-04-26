package no.nav.rekrutteringsbistand.statistikk.datakatalog.alder

import kscience.plotly.Plot
import kscience.plotly.Plotly
import kscience.plotly.bar
import kscience.plotly.toJsonString
import no.nav.rekrutteringsbistand.statistikk.datakatalog.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import kotlin.math.roundToInt

class AlderStatistikk(private val repository: Repository, private val dagensDato: () -> LocalDate) {

    companion object {
        private val filnavnAlderPresentert: String = "alderPresentert.json"
        private val filnavnAlderFåttJobben: String = "alderFåttJobben.json"
        private val filnavnAlderAndelPresentert: String = "alderAndelPresentert.json"
        private val filnavnAlderAndelFåttJobben: String = "alderAndelFåttJobben.json"
        private val fraDatoAlder = LocalDate.of(2021, 4, 8)
    }
    fun views() = listOf(
        View(
            title = "Antall alder presentert",
            description = "Vise antall alder presentert",
            specType = "plotly",
            spec = Spec(
                url = filnavnAlderPresentert
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
                url = filnavnAlderFåttJobben
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

    fun plotlyFiler() = repository.hentAlderDatagrunnlag(dagerMellom(fraDatoAlder, dagensDato())).let { alderDatakatalog ->
        listOf(
            filnavnAlderPresentert to lagPlotAlderPresentert(alderDatakatalog).toJsonString(),
            filnavnAlderFåttJobben to lagPlotAlderFåttJobben(alderDatakatalog).toJsonString(),
            filnavnAlderAndelPresentert to lagPlotAlderAndelPresentert(alderDatakatalog).toJsonString(),
            filnavnAlderAndelFåttJobben to lagPlotAlderAndelFåttJobben(alderDatakatalog).toJsonString()
        )
    }

    private fun Plot.lagBarAlder(hentVerdi: (Aldersgruppe, LocalDate) -> Int, aldersgruppe: Aldersgruppe, description: String) =
        bar {
            val datoer = dagerMellom(fraDatoAlder, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentVerdi(aldersgruppe, it) }
            name = description
        }

    private fun Plot.lagBarAndelAlder(hentVerdi: (LocalDate) -> Double, description: String) = bar {
        val datoer = dagerMellom(fraDatoAlder, dagensDato())
        x.strings = datoer.map { it.toString() }
        y.numbers = datoer.map { (hentVerdi(it) * 100).roundToInt() }
        name = description
    }

    private fun lagPlotAlderPresentert(alderDatagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        log.info("Henter data for alder for datakatalog")

        lagBarAlder(alderDatagrunnlag::hentAntallPresentert, Aldersgruppe.under30, "Antall presentert under 30")
        lagBarAlder(alderDatagrunnlag::hentAntallPresentert, Aldersgruppe.over50, "Antall presentert over 50")
        lagBarAlder(alderDatagrunnlag::hentAntallPresentert, Aldersgruppe.mellom30og50,"Antall presentert mellom 30 og 50")
        getLayout("Antall")
    }

    private fun lagPlotAlderAndelPresentert(alderDatagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        log.info("Henter data for alder for datakatalog")

        lagBarAndelAlder(alderDatagrunnlag::hentAndelPresentertUng, "Andel presentert under 30")
        lagBarAndelAlder(alderDatagrunnlag::hentAndelPresentertSenior, "Andel presentert over 50")
        getLayout("Andel %")
    }

    private fun lagPlotAlderFåttJobben(alderDatagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        log.info("Henter data for alder for datakatalog")

        lagBarAlder(alderDatagrunnlag::hentAntallFåttJobben, Aldersgruppe.under30, "Antall fått jobben under 30")
        lagBarAlder(alderDatagrunnlag::hentAntallFåttJobben, Aldersgruppe.over50, "Antall fått jobben over 50")
        lagBarAlder(alderDatagrunnlag::hentAntallFåttJobben, Aldersgruppe.mellom30og50,"Antall fått jobben mellom 30 og 50")
        getLayout("Antall")
    }

    private fun lagPlotAlderAndelFåttJobben(alderDatagrunnlag: AlderDatagrunnlag) = Plotly.plot {
        log.info("Henter data for alder for datakatalog")

        lagBarAndelAlder(alderDatagrunnlag::hentAndelFåttJobbenUng, "Andel fått jobben under 30")
        lagBarAndelAlder(alderDatagrunnlag::hentAndelFåttJobbenSenior, "Andel fått jobben over 50")
        getLayout("Andel %")
    }
}