package datakatalog

import db.TestDatabase
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
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
    val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.fullPath) {
                    "https://datakatalog-api.dev.intern.nav.no/v1/datapackage/e0745dcae428b0fa4309b3c065f7706b" -> {
                        assertEquals("",request.headers)
                        assertEquals("",request.body)
                        respond("")
                    }
                    else -> error("Unhandled ${request.url.fullPath}")
                }
            }
        }
    }



    val hullICvTilDatakatalogStatistikk = HullICvTilDatakatalogStatistikk(repository, DatakatalogKlient(client))

    @Test
    fun testSendFil()
    {
        hullICvTilDatakatalogStatistikk.run()
    }
}