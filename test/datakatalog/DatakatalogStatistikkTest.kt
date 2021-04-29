package datakatalog

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import datakatalog.plot.testData
import datakatalog.plot.testPlot
import db.TestDatabase
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.rekrutteringsbistand.statistikk.datakatalog.*
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.KandidatutfallRepository
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import kotlin.test.assertTrue
import kotlin.text.String

class DatakatalogStatistikkTest {
    companion object {
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
    }


    private fun MultiPartFormDataContent.hentPartsMedReflection(): List<MultipartPart> {
        val partListe = this.javaClass.getDeclaredField("rawParts").let {
            it.trySetAccessible()
            it.get(this)
        } as ArrayList<*>

        return partListe.map { part ->
            val header = part.javaClass.getDeclaredField("headers").run {
                trySetAccessible()
                get(part)
            } as ByteArray
            val body = part.javaClass.getDeclaredField("provider").run {
                trySetAccessible()
                get(part)
            } as () -> ByteReadPacket
            MultipartPart(String(header), body().readText())
        }
    }

    private class MultipartPart(val header: String, val body: String) {
        fun harFilnavn(filnavn: String) = header.contains(filnavn)
    }

    @Test
    fun `datapakke skal sendes`() {

        fun hullViews() =
            listOf(
                View(
                    title = "Antall hull presentert",
                    description = "Vise antall hull presentert",
                    specType = "plotly",
                    spec = Spec(url = "hullAntallPresentert.json")
                ),
                View(
                    title = "Andel hull presentert",
                    description = "Vise andel hull presentert",
                    specType = "plotly",
                    spec = Spec(url = "hullAndelPresentert.json")
                ),
                View(
                    title = "Antall hull fått jobben",
                    description = "Vise antall fått jobben",
                    specType = "plotly",
                    spec = Spec(url = "hullAntallFåttJobben.json")
                ),
                View(
                    title = "Andel hull fått jobben",
                    description = "Vise andel hull fått jobben",
                    specType = "plotly",
                    spec = Spec(url = "hullAndelFåttJobben.json")
                )
            )

        fun alderViews() = listOf(
            View(
                title = "Antall alder presentert",
                description = "Vise antall alder presentert",
                specType = "plotly",
                spec = Spec(url = "alderAntallPresentert.json")
            ),
            View(
                title = "Andel alder presentert",
                description = "Vise andel alder presentert",
                specType = "plotly",
                spec = Spec(url = "alderAndelPresentert.json")
            ),
            View(
                title = "Antall alder fått jobben",
                description = "Vise antall alder fått jobben",
                specType = "plotly",
                spec = Spec(url = "alderAntallFåttJobben.json")
            ),
            View(
                title = "Andel alder fått jobben",
                description = "Vise andel alder fått jobben",
                specType = "plotly",
                spec = Spec(url = "alderAndelFåttJobben.json")
            )
        )

        var kalt = false
        val client = lagVerifiableHttpClient(datapakkeAsserts = { body ->
            val datapakke = Datapakke(
                title = "Rekrutteringsbistand statistikk",
                description = "Vise rekrutteringsbistand statistikk",
                views = listOf(hullViews(), alderViews()).flatten(),
                resources = emptyList()
            )

            val expected = jacksonObjectMapper().writeValueAsString(datapakke)

            JSONAssert.assertEquals(expected, body, true)
            kalt = true
        })
        lagDatakatalogStatistikk(client).run()
        assertTrue(kalt)
    }

    @Test
    fun `Antall Hull i cv presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    listOf(
                        "Antall presentert med hull",
                        "Antall presentert uten hull",
                        "Antall presentert ukjent om de har hull"
                    )
                )
            )
        )
        verifiserPlotSendt("hullPresentert.json", expected)
    }

    @Test
    fun `Andel Hull i cv presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    listOf(
                        "Andel presentert med hull"
                    )
                )
            )
        )
        verifiserPlotSendt("hullAndelPresentert.json", expected)
    }

    @Test
    fun `Antall Hull i cv fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    listOf(
                        "Antall fått jobben med hull",
                        "Antall fått jobben uten hull",
                        "Antall fått jobben ukjent om de har hull"
                    )
                )
            )
        )
        verifiserPlotSendt("hullFåttJobben.json", expected)
    }

    @Test
    fun `Andel Hull i cv fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    listOf(
                        "Andel fått jobben med hull"
                    )
                )
            )
        )
        verifiserPlotSendt("hullAndelFåttJobben.json", expected)
    }


    @Test
    fun `Antall alder presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    listOf(
                        "Antall presentert under 30",
                        "Antall presentert over 50",
                        "Antall presentert mellom 30 og 50"
                    )
                )
            )
        )
        verifiserPlotSendt("alderPresentert.json", expected)
    }

    @Test
    fun `Andel alder presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    listOf(
                        "Andel presentert under 30",
                        "Andel presentert over 50",
                    )
                )
            )
        )
        verifiserPlotSendt("alderAndelPresentert.json", expected)
    }

    @Test
    fun `Antall alder fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    listOf(
                        "Antall fått jobben under 30",
                        "Antall fått jobben over 50",
                        "Antall fått jobben mellom 30 og 50"
                    )
                )
            )
        )
        verifiserPlotSendt("alderFåttJobben.json", expected)
    }

    @Test
    fun `Andel alder fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    listOf(
                        "Andel fått jobben under 30",
                        "Andel fått jobben over 50",
                    )
                )
            )
        )
        verifiserPlotSendt("alderAndelFåttJobben.json", expected)
    }

    fun verifiserPlotSendt(filnavn: String, expectedJson: String) {
        var kalt = false
        val client = lagVerifiableHttpClient(attachementAsserts = { partListe ->
            partListe.filter { it.harFilnavn(filnavn) }
                .forEach { JSONAssert.assertEquals(expectedJson, it.body, true) }
            kalt = true
        })

        lagDatakatalogStatistikk(client).run()
        assertTrue(kalt)
    }

    private fun lagDatakatalogStatistikk(client: HttpClient) = DatakatalogStatistikk(
        repository,
        DatakatalogKlient(client, DatakatalogUrl(Cluster.LOKAL)),
        dagensDato = { LocalDate.of(2021, 4, 20) })

    private fun lagVerifiableHttpClient(
        attachementAsserts: (List<MultipartPart>) -> Unit = {},
        datapakkeAsserts: (String) -> Unit = {}
    ) =
        HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/v1/datapackage/10d33ba3796b95b53ac1466015aa0ac7/attachments" -> {
                            val multiPartFormDataContent = request.body as MultiPartFormDataContent
                            val actualPartsInput = multiPartFormDataContent.hentPartsMedReflection()
                            attachementAsserts(actualPartsInput)
                            respond("")
                        }
                        "/v1/datapackage/10d33ba3796b95b53ac1466015aa0ac7" -> {
                            datapakkeAsserts(String(request.body.toByteArray()))
                            respond("")
                        }

                        else -> error("Unhandled ${request.url.fullPath}")
                    }
                }
            }
        }
}
