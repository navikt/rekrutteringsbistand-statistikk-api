package statistikkapi.stillinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import statistikkapi.Cluster
import statistikkapi.BearerToken
import statistikkapi.log
import java.time.LocalDateTime

interface ElasticSearchKlient {
    fun hentStilling(stillingUuid: String): ElasticSearchStilling?
}

class ElasticSearchKlientImpl(private val httpKlient: HttpClient = HttpClient(Apache),
                              private val tokenProvider: () -> BearerToken
): ElasticSearchKlient {

    private val stillingssokProxyCluster = if (Cluster.current == Cluster.PROD_FSS) "prod-gcp" else "dev-gcp"
    private val token: BearerToken
        get() = tokenProvider()

    private val stillingssokProxyDokumentUrl = when (Cluster.current) {
        Cluster.PROD_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.intern.nav.no/stilling/_doc"
        Cluster.DEV_FSS -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
        Cluster.LOKAL -> "https://rekrutteringsbistand-stillingssok-proxy.dev.intern.nav.no/stilling/_doc"
    }

    override fun hentStilling(stillingUuid: String): ElasticSearchStilling? = runBlocking {
        val esSvar: String = httpKlient.get("$stillingssokProxyDokumentUrl/$stillingUuid") {
            headers(token.leggTilBearerToken())
        }
        mapElasticSearchJsonSvarTilStilling(esSvar)
    }

    private fun mapElasticSearchJsonSvarTilStilling(fulltElasticSearchSvarJson: String): ElasticSearchStilling? {
        val harFåttTreff = jacksonObjectMapper().readTree(fulltElasticSearchSvarJson).at("/found").asBoolean()

        if (!harFåttTreff) {
            return null
        }

        val jsonStilling = jacksonObjectMapper().readTree(fulltElasticSearchSvarJson)
            .at("/_source")?.at("/stilling")!!

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

    companion object {
        val stillingssokProxyScope = "api://${if (Cluster.current == Cluster.PROD_FSS) "prod-gcp" else "dev-gcp"}.arbeidsgiver.rekrutteringsbistand-stillingssok-proxy/.default"
    }
}

private fun JsonNode.asLocalDateTime() = LocalDateTime.parse(this.asText())
