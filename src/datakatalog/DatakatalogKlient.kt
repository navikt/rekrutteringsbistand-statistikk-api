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
                        private val url: DatakatalogUrl) {
    fun sendPlotlyFilTilDatavarehus(plotlyJsons: List<Pair<String, String>>) {
        runBlocking {
            val response: HttpResponse = httpClient
                .put(url.ressursfil()) {
                    body = MultiPartFormDataContent(
                        formData {
                            plotlyJsons.forEach { plotlyJson ->
                                this.append("files", plotlyJson.second,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                                        append(HttpHeaders.ContentDisposition, " filename=${plotlyJson.first}")
                                    })
                            }
                        }
                    )
                }
            log.info("Svar fra datakatalog filapi $response")
        }
    }
    fun sendDatapakke(lagDatapakke: Datapakke) {
        runBlocking {
            val response: HttpResponse = httpClient
                .put(url.datapakke()) {
                    body = lagDatapakke
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            log.info("Svar fra datakatalog datapakke api $response")
        }
    }
}