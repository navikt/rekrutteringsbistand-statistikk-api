package no.nav.rekrutteringsbistand.statistikk.datakatalog

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kscience.plotly.*
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.log
import java.time.LocalDate


fun sendHullICvTilDatakatalog(repository: Repository) = Runnable {
    log.info("Henter data for hull for katakatalog")
    val fraDato = LocalDate.of(2021, 4, 6)
    val datoer: List<LocalDate> = finnDagerFra(fraDato)

    log.info("Henter data for hull for katakatalog for dager: $datoer")

    datoer.forEach {
        val fåttJobbenMedHull = repository.hentAntallFåttJobben(harHull = true, it, it.plusDays(1))
        val fåttJobbenUtenHull = repository.hentAntallFåttJobben(harHull = false, it, it.plusDays(1))
        val fåttJobbenUkjentHull = repository.hentAntallFåttJobben(harHull = null, it, it.plusDays(1))

        val presentertMedHull = repository.hentAntallPresentert(harHull = true, it, it.plusDays(1))
        val presentertUtenHull = repository.hentAntallPresentert(harHull = false, it, it.plusDays(1))
        val presentertUkjentHull = repository.hentAntallPresentert(harHull = null, it, it.plusDays(1))

        log.info(
            "Har hentet data for hull for datakatalog for $it: " +
                    "fåttJobbenMedHull: $fåttJobbenMedHull, " +
                    "fåttJobbenUtenHull: $fåttJobbenUtenHull, " +
                    "fåttJobbenUkjentHull: $fåttJobbenUkjentHull, " +
                    "presentertMedHull: $presentertMedHull, " +
                    "presentertUtenHull: $presentertUtenHull, " +
                    "presentertUkjentHull: $presentertUkjentHull"
        )
    }

    val plot = Plotly.plot {
        histogram {
            x.strings = datoer.map { it.toString() }
            y.numbers = datoer.map { repository.hentAntallPresentert(harHull = true, it, it.plusDays(1)) }
            name = "Antall presentert med hull"
        }

        getLayout()
    }
    val plotlyJson = plot.toJsonString()
    log.info("Plotty klargjort: ${plotlyJson}")

    //e0745dcae428b0fa4309b3c065f7706b
    fun datapakkeHttpClient() = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        defaultRequest {
            //contentType(ContentType.Json)
        }
    }

    runBlocking {
        val response: HttpResponse = datapakkeHttpClient()
            .put("https://datakatalog-api.dev.intern.nav.no/v1/datapackage/e0745dcae428b0fa4309b3c065f7706b/attachments") {
                body = MultiPartFormDataContent(
                    formData {
                        this.append("files", plot.toJsonString(),
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Json)
                                append(HttpHeaders.ContentDisposition, " filename=antallhull.json")
                            })
                    }
                )
            }
        log.info("Svar fra datakatalog filapi $response")
    }

    runBlocking {
        val datapakke = Datapakke(
            title = "Hull i cv",
            description = "Vise hull i cv",
            resources = emptyList(),
            views = listOf(
                View(
                    title = "Antall hull i cv",
                    description = "Vise antall hull i cv",
                    specType = "plotly",
                    spec = Spec(
                        url = "antallhull.json"
                    )
                )
            )
        )

        val response: HttpResponse = datapakkeHttpClient()
            .put("https://datakatalog-api.dev.intern.nav.no/v1/datapackage/e0745dcae428b0fa4309b3c065f7706b") {
                body = datapakke
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
        log.info("Svar fra datakatalog datapakke api $response")
    }




    return@Runnable
}


private fun Plot.getLayout() {
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
                text = "Antall"
                font {
                    size = 16
                }
            }
        }
    }
}

private tailrec fun finnDagerFra(dato: LocalDate): List<LocalDate> =
    if (dato.isAfter(LocalDate.now())) emptyList()
    else listOf(dato) + finnDagerFra(dato.plusDays(1))
