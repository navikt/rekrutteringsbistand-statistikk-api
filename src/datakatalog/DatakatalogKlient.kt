package no.nav.rekrutteringsbistand.statistikk.datakatalog

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kscience.plotly.toJsonString
import no.nav.rekrutteringsbistand.statistikk.log

private fun datapakkeHttpClient() = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

class DatakatalogKlient(private val httpClient: HttpClient = datapakkeHttpClient(),
                        private val rootURL: String = "https://datakatalog-api.dev.intern.nav.no/v1/datapackage/",
                        private val datapakkeId: String = "e0745dcae428b0fa4309b3c065f7706b") {

    fun sendPlotlyFilTilDatavarehus(plotlyJson: String) {
        runBlocking {
            val response: HttpResponse = httpClient
                .put("$rootURL$datapakkeId/attachments") {
                    body = MultiPartFormDataContent(
                        formData {
                            this.append("files", plotlyJson,
                                Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                                    append(HttpHeaders.ContentDisposition, " filename=${HullICvTilDatakatalogStatistikk.filnavn}")
                                })
                        }
                    )
                }
            log.info("Svar fra datakatalog filapi $response")
        }
    }
    fun sendDatapakke(lagDatapakke: Datapakke) {
        runBlocking {
            val response: HttpResponse = httpClient
                .put("$rootURL$datapakkeId") {
                    body = lagDatapakke
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            log.info("Svar fra datakatalog datapakke api $response")
        }
    }
}