package datakatalog

import assertk.assertThat
import db.TestDatabase
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.forms.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.util.*
import io.mockk.InternalPlatformDsl.toArray
import io.mockk.InternalPlatformDsl.toStr
import no.nav.rekrutteringsbistand.statistikk.datakatalog.DatakatalogKlient
import no.nav.rekrutteringsbistand.statistikk.datakatalog.HullICvTilDatakatalogStatistikk
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import org.junit.Test
import kotlin.test.assertEquals

class HullICvTilDatakatalogStatistikkTest {
    companion object {
        private val database = TestDatabase()
        private val repository = Repository(database.dataSource)
    }

    val jsonHeader = Headers.build { this.append(HttpHeaders.ContentType, ContentType.Application.Json) }
    val utf8Header = Headers.build {
        this.apply {
            this.append(HttpHeaders.AcceptCharset, Charsets.UTF_8.toString())
            this.append(HttpHeaders.Accept, "*/*")
            this.append(HttpHeaders.ContentDisposition, "*/*")

        }
    }
    val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.fullPath) {
                    "/v1/datapackage/e0745dcae428b0fa4309b3c065f7706b/attachments" -> {
                        val a = String((request.body as MultiPartFormDataContent).toByteArray())
                       // ((request.body as MultiPartFormDataContent).rawParts_field.get(0).provider_field)
                        println(a)
                        /*assertThat{a.contains("bgggggggg")}
                        println(a)*/
                        assertEquals(utf8Header, request.headers)
                        assertEquals("a", String((request.body as MultiPartFormDataContent).toByteArray()))
                        respond("")
                    }
                    "/v1/datapackage/e0745dcae428b0fa4309b3c065f7706b" -> {
                        assertEquals(utf8Header, request.headers)
                        assertEquals("", request.body.toString())
                        respond("")
                    }

                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }


    val hullICvTilDatakatalogStatistikk = HullICvTilDatakatalogStatistikk(repository, DatakatalogKlient(client))

    @Test
    fun testSendFil() {
        hullICvTilDatakatalogStatistikk.run()
    }
}