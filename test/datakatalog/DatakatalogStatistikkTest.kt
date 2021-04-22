package datakatalog

import assertk.assertThat
import assertk.assertions.contains
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import db.TestDatabase
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogKlient
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogUrl
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class DatakatalogStatistikkTest {
    companion object {
        private val database = TestDatabase()
        private val repository = Repository(database.dataSource)
    }

    val client = HttpClient(MockEngine) {
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
                        val actual = String((request.body as MultiPartFormDataContent).toByteArray())
                        val expectedAntall =
                            """{"layout":{"xaxis":{"title":{"text":"Dato","font":{"size":16}}},"bargap":0.1,"title":{"text":"Basic Histogram","font":{"size":20,"color":"black"}},"yaxis":{"title":{"text":"Antall","font":{"size":16}}}},"data":[{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall presentert med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall presentert uten hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall presentert ukjent om de har hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall fått jobben med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall fått jobben uten hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Antall fått jobben ukjent om de har hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"}]}"""
                        assertThat(actual).contains(expectedAntall)
                        val expectedAndel =
                            """{"layout":{"xaxis":{"title":{"text":"Dato","font":{"size":16}}},"bargap":0.1,"title":{"text":"Basic Histogram","font":{"size":20,"color":"black"}},"yaxis":{"title":{"text":"Antall","font":{"size":16}}}},"data":[{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Andel presentert med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-08","2021-04-09","2021-04-10","2021-04-11","2021-04-12","2021-04-13","2021-04-14","2021-04-15","2021-04-16","2021-04-17","2021-04-18","2021-04-19","2021-04-20"],"name":"Andel fått jobben med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"}]}"""
                        assertThat(actual).contains(expectedAndel)

                        respond("")
                    }
                    "/v1/datapackage/10d33ba3796b95b53ac1466015aa0ac7" -> {
                        val expected =
                            """{"title":"Hull i cv","description":"Vise hull i cv","views":[{"title":"Antall hull i cv","description":"Vise antall hull i cv","specType":"plotly","spec":{"url":"antallhull.json"}},{"title":"Andel hull i cv","description":"Vise andel hull i cv","specType":"plotly","spec":{"url":"andelhull.json"}}],"resources":[]}"""
                        assertEquals(expected, String(request.body.toByteArray()))
                        respond("")
                    }

                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }


    val DatakatalogStatistikk = DatakatalogStatistikk(repository, DatakatalogKlient(client, DatakatalogUrl(Cluster.LOKAL)), dagensDato = { LocalDate.of(2021, 4, 20)})

    @Test
    fun testSendFil() {
        DatakatalogStatistikk.run()
    }
}