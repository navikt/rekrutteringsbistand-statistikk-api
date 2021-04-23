package no.nav.rekrutteringsbistand.statistikk.datakatalog

import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt


class DatakatalogStatistikk(
    private val repository: Repository, private val datakatalogKlient: DatakatalogKlient,
    private val dagensDato: () -> LocalDate
) : Runnable {

    companion object {
        private val filnavnAntallHull: String = "antallhull.json"
        private val filnavnAndelHull: String = "andelhull.json"
        private val fraDatoHull = LocalDate.of(2021, 4, 8)

        private val filnavnAlderPresentert: String = "alderPresentert.json"
        private val filnavnAlderFåttJobben: String = "aalderFåttJobben.json"
        private val filnavnAlderAndelPresentert: String = "alderAndelPresentert.json"
        private val filnavnAlderAndelFåttJobben: String = "alderAndelFåttJobben.json"
        private val fraDatoAlder = LocalDate.of(2021, 4, 8)
    }

    override fun run() {
        datakatalogKlient.sendPlotlyFilTilDatavarehus(
            filnavnAntallHull to lagPlotAntallHull().toJsonString(),
            filnavnAndelHull to lagPlotAndelHull().toJsonString(),
            filnavnAlderPresentert to lagPlotAlderPresentert().toJsonString(),
            filnavnAlderFåttJobben to lagPlotAlderFåttJobben().toJsonString(),
            filnavnAlderAndelPresentert to lagPlotAlderAndelPresentert().toJsonString(),
            filnavnAlderAndelFåttJobben to lagPlotAlderAndelFåttJobben().toJsonString(),
        )
        datakatalogKlient.sendDatapakke(lagDatapakke())
    }

    private fun lagDatapakke() = Datapakke(
        title = "Rekrutteringsbistand statistikk",
        description = "Vise rekrutteringsbistand statistikk",
        resources = emptyList(),
        views = listOf(
            View(
                title = "Antall hull i cv",
                description = "Vise antall hull i cv",
                specType = "plotly",
                spec = Spec(
                    url = filnavnAntallHull
                )
            ),
            View(
                title = "Andel hull i cv",
                description = "Vise andel hull i cv",
                specType = "plotly",
                spec = Spec(
                    url = filnavnAndelHull
                )
            ),
            View(
                title = "Antall alder presentert",
                description = "Vise antal alder presentert",
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
                description = "Vise antal alder fått jobben",
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
    )


    private fun lagPlotAntallHull() = Plotly.plot {
        fun Plot.lagBarAntallHull(hentVerdi: (Boolean?, LocalDate) -> Int, harHull: Boolean?, description: String) =
            bar {
                val datoer = dagerMellom(fraDatoHull, dagensDato())
                x.strings = datoer.map { it.toString() }
                y.numbers = datoer.map { hentVerdi(harHull, it) }
                name = description
            }

        val hullDatakatalog = repository.hentHullDatagrunnlag(dagerMellom(fraDatoHull, dagensDato()))
        log.info("Henter data for hull for datakatalog")

        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, true, "Antall presentert med hull")
        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, false, "Antall presentert uten hull")
        lagBarAntallHull(hullDatakatalog::hentAntallPresentert, null, "Antall presentert ukjent om de har hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, true, "Antall fått jobben med hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, false, "Antall fått jobben uten hull")
        lagBarAntallHull(hullDatakatalog::hentAntallFåttJobben, null, "Antall fått jobben ukjent om de har hull")
        getLayout("Antall")
    }

    private fun lagPlotAndelHull() = Plotly.plot {
        fun Plot.lagBarAndelHull(hentVerdi: (LocalDate) -> Double, description: String) = bar {
            val datoer = dagerMellom(fraDatoHull, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { (hentVerdi(it) * 100).roundToInt() }
            name = description
        }

        val hullDatakatalog = repository.hentHullDatagrunnlag(dagerMellom(fraDatoHull, dagensDato()))
        log.info("Henter data for hull for datakatalog")

        lagBarAndelHull(hullDatakatalog::hentAndelPresentert, "Andel presentert med hull")
        lagBarAndelHull(hullDatakatalog::hentAndelFåttJobben, "Andel fått jobben med hull")
        getLayout("Andel %")
    }

    fun Plot.lagBarAlder(hentVerdi: (Aldersgruppe, LocalDate) -> Int, aldersgruppe: Aldersgruppe, description: String) =
        bar {
            val datoer = dagerMellom(fraDatoAlder, dagensDato())
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { hentVerdi(aldersgruppe, it) }
            name = description
        }

    private fun lagPlotAlderPresentert() = Plotly.plot {

        val alderDatakatalog = repository.hentAlderDatagrunnlag(dagerMellom(fraDatoAlder, dagensDato()))
        log.info("Henter data for alder for datakatalog")

        lagBarAlder(alderDatakatalog::hentAntallPresentert, Aldersgruppe.under30, "Antall presentert under 30")
        lagBarAlder(alderDatakatalog::hentAntallPresentert, Aldersgruppe.over50, "Antall presentert over 50")
        lagBarAlder(
            alderDatakatalog::hentAntallPresentert,
            Aldersgruppe.mellom30og50,
            "Antall presentert mellom 30 og 50"
        )
        getLayout("Antall")
    }

    fun Plot.lagBarAndelAlder(hentVerdi: (LocalDate) -> Double, description: String) = bar {
        val datoer = dagerMellom(fraDatoAlder, dagensDato())
        x.strings = datoer.map { it.toString() }
        y.numbers = datoer.map { (hentVerdi(it) * 100).roundToInt() }
        name = description
    }

    private fun lagPlotAlderAndelPresentert() = Plotly.plot {
        val alderDatakatalog = repository.hentAlderDatagrunnlag(dagerMellom(fraDatoAlder, dagensDato()))
        log.info("Henter data for alder for datakatalog")

        lagBarAndelAlder(alderDatakatalog::hentAndelPresentertUng, "Andel presentert under 30")
        lagBarAndelAlder(alderDatakatalog::hentAndelPresentertSenior, "Andel presentert over 50")
        getLayout("Andel %")
    }

    private fun lagPlotAlderFåttJobben() = Plotly.plot {

        val alderDatakatalog = repository.hentAlderDatagrunnlag(dagerMellom(fraDatoAlder, dagensDato()))
        log.info("Henter data for alder for datakatalog")

        lagBarAlder(alderDatakatalog::hentAntallFåttJobben, Aldersgruppe.under30, "Antall fått jobben under 30")
        lagBarAlder(alderDatakatalog::hentAntallFåttJobben, Aldersgruppe.over50, "Antall fått jobben over 50")
        lagBarAlder(
            alderDatakatalog::hentAntallFåttJobben,
            Aldersgruppe.mellom30og50,
            "Antall fått jobben mellom 30 og 50"
        )
        getLayout("Antall")
    }

    private fun lagPlotAlderAndelFåttJobben() = Plotly.plot {
        val alderDatakatalog = repository.hentAlderDatagrunnlag(dagerMellom(fraDatoAlder, dagensDato()))
        log.info("Henter data for alder for datakatalog")

        lagBarAndelAlder(alderDatakatalog::hentAndelFåttJobbenUng, "Andel fått jobben under 30")
        lagBarAndelAlder(alderDatakatalog::hentAndelFåttJobbenSenior, "Andel fått jobben over 50")
        getLayout("Andel %")
    }

}


private fun Plot.getLayout(yTekst: String) {
    layout {
        bargap = 0.1
        title {
            text = "Basic Histogram"
            font {
                size = 20
                color("black")
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

private fun dagerMellom(fraDato: LocalDate, tilDato: LocalDate) = Period.between(fraDato, tilDato).days
    .let { antallDager ->
        (0..antallDager).map { fraDato + Period.ofDays(it) }
    }