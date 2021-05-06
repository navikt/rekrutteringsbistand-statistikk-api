package statistikkapi.datakatalog

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import statistikkapi.Cluster
import statistikkapi.datakatalog.plot.testData
import statistikkapi.datakatalog.plot.testPlot
import statistikkapi.db.TestDatabase
import statistikkapi.db.TestRepository
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.kandidatutfall.OpprettKandidatutfall
import statistikkapi.kandidatutfall.Utfall
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.assertTrue
import kotlin.text.String

class DatakatalogStatistikkTest {
    companion object {
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val dagensDato = { LocalDate.of(2021, 5, 5) }
        private val testRepository = TestRepository(database.dataSource)
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

        fun tilretteleggingsbehovViews() = listOf(
            View(
                title = "Antall med forskjellige tilretteleggingsbehov presentert",
                description = "Vise antall med forskjellige tilretteleggingsbehov presentert",
                specType = "plotly",
                spec = Spec(url = "tilretteleggingsbehovAntallPresentert.json")
            ),
            View(
                title = "Andel med minst et tilretteleggingsbehov presentert",
                description = "Vise andel med minst et tilretteleggingsbehov presentert",
                specType = "plotly",
                spec = Spec(url = "tilretteleggingsbehovAndelPresentert.json")
            ),

            View(
                title = "Antall med forskjellige tilretteleggingsbehov som har fått jobben",
                description = "Vise antall med forskjellige tilretteleggingsbehov som har fått jobben",
                specType = "plotly",
                spec = Spec(url = "tilretteleggingsbehovAntallFåttJobben.json")
            ),
            View(
                title = "Andel med minst et tilretteleggingsbehov som har fått jobben",
                description = "Vise andel med minst et tilretteleggingsbehov som har fått jobben",
                specType = "plotly",
                spec = Spec(url = "tilretteleggingsbehovAndelFåttJobben.json")
            )
        )

        var kalt = false
        val client = lagVerifiableHttpClient(datapakkeAsserts = { body ->
            val datapakke = Datapakke(
                title = "Rekrutteringsbistand statistikk",
                description = "Vise rekrutteringsbistand statistikk",
                views = listOf(hullViews(), alderViews(), tilretteleggingsbehovViews()).flatten(),
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
        repository.lagreUtfall(lagKandidat(harHull=true),datotid(11))
        repository.lagreUtfall(lagKandidat(harHull=true),datotid(12))
        repository.lagreUtfall(lagKandidat(harHull=null),datotid(12))
        repository.lagreUtfall(lagKandidat(harHull=false),datotid(13))
        repository.lagreUtfall(lagKandidat(harHull=true,utfall = Utfall.FATT_JOBBEN),datotid(13))
        nyUnikPersonKandidatListeKombo().let { unikKombo ->
            repository.lagreUtfall(lagKandidat(unikKombo, harHull = true), datotid(14))
            repository.lagreUtfall(lagKandidat(unikKombo, harHull = null), datotid(14, time = 6))
        }
        nyUnikPersonKandidatListeKombo().let { unikKombo ->
            repository.lagreUtfall(lagKandidat(unikKombo, harHull = true), datotid(15))
            repository.lagreUtfall(lagKandidat(unikKombo, harHull = false), datotid(16, time = 7))
        }
        nyUnikPersonKandidatListeKombo().let { unikKombo ->
            repository.lagreUtfall(lagKandidat(unikKombo, harHull=true),datotid(17))
            repository.lagreUtfall(lagKandidat(unikKombo, harHull=null, utfall = Utfall.FATT_JOBBEN),datotid(18,time = 8))
        }
        repeat(3) { repository.lagreUtfall(lagKandidat(harHull = true), datotid(20)) }
        repeat(4) { repository.lagreUtfall(lagKandidat(harHull = null), datotid(20)) }
        repeat(5) { repository.lagreUtfall(lagKandidat(harHull = false), datotid(20)) }

        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    LocalDate.of(2021, 4, 8) til dagensDato(),
                    listOf(
                        "Antall presentert med hull",
                        "Antall presentert uten hull",
                        "Antall presentert ukjent om de har hull"
                    ),
                    listOf(
                        mapOf(dato(11) to 1, dato(12) to 1, dato(13) to 1, dato(14) to 1, dato(15) to 1, dato(17) to 1, dato(20) to 3),
                        mapOf(dato(13) to 1, dato(20) to 5),
                        mapOf(dato(12) to 1, dato(20) to 4)
                    )
                )
            )
        )
        verifiserPlotSendt("hullPresentert.json", expected)
    }

    private fun datotid(dag:Int, måned:Int=4, år:Int=2021, time:Int=0, minutt:Int=0, sekund:Int=0) =
        LocalDateTime.of(år, måned, dag, time, minutt, sekund)
    private fun dato(dag:Int, måned:Int=4, år:Int=2021) =
        LocalDate.of(år, måned, dag)

    private fun lagKandidat(aktørIdKandidatliste: Pair<String,String> = nyUnikPersonKandidatListeKombo(), utfall: Utfall= Utfall.PRESENTERT, navIdent:String="A123456", navKontor:String="etkontor",
                            stillingsId:String="11235", synligKandidat:Boolean=true, harHull:Boolean?=null, alder:Int?=35, tilretteleggingsbehov:List<String> =emptyList()) =
        OpprettKandidatutfall(aktørIdKandidatliste.first,utfall,navIdent,navKontor,aktørIdKandidatliste.second,stillingsId,synligKandidat,harHull,alder,tilretteleggingsbehov)

    private var aktørIdTeller=0
    private var kandidatlisteIdTeller=0
    private fun nyUnikPersonKandidatListeKombo() = aktørIdTeller.toString() to kandidatlisteIdTeller.toString()
        .also { oppdaterTeller() }

    private fun oppdaterTeller() {
        if(aktørIdTeller>kandidatlisteIdTeller) kandidatlisteIdTeller++
        else aktørIdTeller++
    }

    @Test
    fun `Andel Hull i cv presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
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
                    LocalDate.of(2021, 4, 8) til dagensDato(),
                    listOf(
                        "Andel fått jobben under 30",
                        "Andel fått jobben over 50",
                    )
                )
            )
        )
        verifiserPlotSendt("alderAndelFåttJobben.json", expected)
    }

    @Test
    fun `Antall tilretteleggingsbehov presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    LocalDate.of(2021, 5, 4) til dagensDato(),
                    listOf()
                )
            )
        )
        verifiserPlotSendt("tilretteleggingsbehovAntallPresentert.json", expected)
    }

    @Test
    fun `Andel tilretteleggingsbehov presentert skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    LocalDate.of(2021, 5, 4) til dagensDato(),
                    listOf("Andel presentert med minst et tilretteleggingsbehov")
                )
            )
        )
        verifiserPlotSendt("tilretteleggingsbehovAndelPresentert.json", expected)
    }

    @Test
    fun `Antall tilretteleggingsbehov fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Antall",
                data = testData(
                    LocalDate.of(2021, 5, 4) til dagensDato(),
                    listOf()
                )
            )
        )
        verifiserPlotSendt("tilretteleggingsbehovAntallFåttJobben.json", expected)
    }

    @Test
    fun `Andel tilretteleggingsbehov fått jobben skal sendes`() {
        val expected = jacksonObjectMapper().writeValueAsString(
            testPlot(
                yaxisText = "Andel %",
                data = testData(
                    LocalDate.of(2021, 5, 4) til dagensDato(),
                    listOf("Andel fått jobben med minst et tilretteleggingsbehov")
                )
            )
        )
        verifiserPlotSendt("tilretteleggingsbehovAndelFåttJobben.json", expected)
    }

    @AfterTest
    fun resetDatabase() {
        testRepository.slettAlleUtfall()
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
        dagensDato)

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
