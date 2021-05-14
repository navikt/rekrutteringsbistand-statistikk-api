package statistikkapi.stillinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import statistikkapi.Cluster
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface ElasticSearchKlient {
    fun hentStilling(stillingUuid: String): ElasticSearchStilling?
}

class ElasticSearchKlientImpl(private val httpKlient: HttpClient = HttpClient(Apache)): ElasticSearchKlient {

    // TODO: Metode for å hente alle stillinger med tilrettelegging/inkludering i gitt tidsrom

    private val stillingssokProxyDokumentUrl = when (Cluster.current) {
        Cluster.PROD_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.intern.nav.no/stilling/_doc"
        Cluster.DEV_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
        Cluster.LOKAL -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
    }

    override fun hentStilling(stillingUuid: String): ElasticSearchStilling? = runBlocking {
        val esSvar: String = httpKlient.get("$stillingssokProxyDokumentUrl/$stillingUuid")
        mapElasticSearchJsonSvarTilStilling(esSvar)
    }

    private fun mapElasticSearchJsonSvarTilStilling(fulltElasticSearchSvarJson: String): ElasticSearchStilling? {

        val jsonStilling = jacksonObjectMapper().readTree(fulltElasticSearchSvarJson)
            .at("/hits/hits")[0]
            ?.at("/_source/stilling") ?: return null

        val (inkluderingsmuligheter, prioriterteMålgrupper, tiltakEllerVirkemidler) = hentTags(jsonStilling.at("/properties/tags").toList().map { it.asText() })

        return ElasticSearchStilling(
                uuid = jsonStilling.at("/uuid").asText(),
                opprettet = jsonStilling.at("/created").asLocalDateTime(),
                publisert = jsonStilling.at("/published").asLocalDateTime(),
                inkluderingsmuligheter = inkluderingsmuligheter,
                prioriterteMålgrupper = prioriterteMålgrupper,
                tiltakEllerEllerVirkemidler = tiltakEllerVirkemidler
            )
    }

    private fun hentTags(ukategoriserteTags: List<String>): Triple<List<InkluderingTag>, List<PrioriterteMålgrupperTag>, List<TiltakEllerVirkemiddelTag>> =
        Triple(
            ukategoriserteTags.filter { InkluderingTag.erGyldig(it) }.map { InkluderingTag.fraNavn(it) },
            ukategoriserteTags.filter { PrioriterteMålgrupperTag.erGyldig(it) }.map { PrioriterteMålgrupperTag.fraNavn(it) },
            ukategoriserteTags.filter { TiltakEllerVirkemiddelTag.erGyldig(it) }.map { TiltakEllerVirkemiddelTag.fraNavn(it) }
        )

}

private fun JsonNode.asLocalDateTime() = LocalDateTime.parse(this.asText())
