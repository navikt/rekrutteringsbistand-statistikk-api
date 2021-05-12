package statistikkapi.stillinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import statistikkapi.Cluster
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ElasticSearchKlient(private val httpKlient: HttpClient = HttpClient(Apache)) {

    // TODO: Metode for å hente alle stillinger med tilrettelegging/inkludering i gitt tidsrom

    val stillingssokProxyDokumentUrl = when (Cluster.current) {
        Cluster.PROD_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.intern.nav.no/stilling/_doc"
        Cluster.DEV_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
        Cluster.LOKAL -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
    }

    fun hentStilling(stillingUuid: String): ElasticSearchStilling? = getRequest("$stillingssokProxyDokumentUrl/$stillingUuid")

    private fun mapElasticSearchJsonSvarTilStilling(fulltElasticSearchSvarJson: String): ElasticSearchStilling? {

        val jsonStilling = jacksonObjectMapper().readTree(fulltElasticSearchSvarJson)
            .at("/hits/hits")[0]
            ?.at("/_source/stilling") ?: return null

        val tags = hentTags(jsonStilling.at("/properties/tags").toList().map { it.asText() })
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        // TODO: Sjekk datohåndtering
        return ElasticSearchStilling(
            jsonStilling.at("/uuid").asText(),
            LocalDateTime.parse(jsonStilling.at("/created").asText().substring(0, 19), dateTimeFormatter),
            LocalDateTime.parse(jsonStilling.at("/published").asText().substring(0, 19), dateTimeFormatter),
            tags.first,
            tags.second,
            tags.third
        )
    }

    private fun hentTags(ukategoriserteTags: List<String>): Triple<List<InkluderingTag>, List<PrioriterteMålgrupperTag>, List<TiltakEllerVirkemiddelTag>> =
        Triple(
            ukategoriserteTags.filter { it.startsWith("INKLUDERING__") }.map { InkluderingTag.valueOf(it.removePrefix("INKLUDERING__")) },
            ukategoriserteTags.filter { it.startsWith("PRIORITERT_MÅLGRUPPE__")}.map { PrioriterteMålgrupperTag.valueOf(it.removePrefix("PRIORITERT_MÅLGRUPPE__"))},
            ukategoriserteTags.filter { it.startsWith("TILTAK_ELLER_VIRKEMIDDEL__")}.map { TiltakEllerVirkemiddelTag.valueOf(it.removePrefix("TILTAK_ELLER_VIRKEMIDDEL__"))}
        )

    private fun getRequest(url: String): ElasticSearchStilling? {
        return runBlocking {
            val client = httpKlient
            val esSvar: String = client.get(url)
            client.close()
            mapElasticSearchJsonSvarTilStilling(esSvar)
        }
    }
}
