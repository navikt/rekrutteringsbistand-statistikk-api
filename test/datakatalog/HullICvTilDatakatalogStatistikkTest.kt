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
import no.nav.rekrutteringsbistand.statistikk.datakatalog.HullICvTilDatakatalogStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import org.junit.Test
import kotlin.test.assertEquals

class HullICvTilDatakatalogStatistikkTest {
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
                        val expected =
                            """{"layout":{"xaxis":{"title":{"text":"Dato","font":{"size":16}}},"bargap":0.1,"title":{"text":"Basic Histogram","font":{"size":20,"color":"black"}},"yaxis":{"title":{"text":"Antall","font":{"size":16}}}},"data":[{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall presentert med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall presentert uten hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall presentert ukjent om de har hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall fått jobben med hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall fått jobben uten hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"},{"x":["2021-04-06","2021-04-05","2021-04-04","2021-04-03","2021-04-02","2021-04-01","2021-03-31","2021-03-30","2021-03-29","2021-03-28","2021-03-27","2021-03-26","2021-03-25","2021-03-24","2021-03-23"],"name":"Antall fått jobben ukjent om de har hull","y":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],"type":"bar"}]}"""
                        assertThat(actual).contains(expected)

                        respond("")
                    }
                    "/v1/datapackage/10d33ba3796b95b53ac1466015aa0ac7" -> {
                        val expected =
                            """{"title":"Hull i cv","description":"Vise hull i cv","views":[{"title":"Antall hull i cv","description":"Vise antall hull i cv","specType":"plotly","spec":{"url":"antallhull.json"}}],"resources":[]}"""
                        assertEquals(expected, String(request.body.toByteArray()))
                        respond("")
                    }

                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }


    val hullICvTilDatakatalogStatistikk = HullICvTilDatakatalogStatistikk(repository, DatakatalogKlient(client, DatakatalogUrl(Cluster.LOKAL)))

    @Test
    fun testSendFil() {
        hullICvTilDatakatalogStatistikk.run()
    }
}