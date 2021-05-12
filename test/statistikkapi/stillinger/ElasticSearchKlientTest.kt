package statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import org.junit.Test
import statistikkapi.etElasticSearchSvarForEnStilling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ElasticSearchKlientImplTest {

    @Test
    fun `skal kunne parse JSON fra stillingssøk-proxy til Stilling-objekt`() {
        val uuid = UUID.randomUUID().toString()
        val publiseringsdato = "2019-01-17T17:08:11"
        val inkluderingstags = InkluderingTag.values().toList()
        val prioriterteMålgrupperTags = PrioriterteMålgrupperTag.values().toList()
        val tiltakVirkemiddelTags = TiltakEllerVirkemiddelTag.values().toList()
        val klient = ElasticSearchKlientImpl(httpClientSøketreff(uuid, publiseringsdato, inkluderingstags, prioriterteMålgrupperTags, tiltakVirkemiddelTags))

        val stilling = klient.hentStilling(uuid)

        assertThat(stilling).isNotNull()
        assertThat(stilling!!.uuid).isEqualTo(uuid)
        assertThat(stilling.publisert.toString()).isEqualTo(publiseringsdato)
        assertThat(stilling.inkluderingsmuligheter).isEqualTo(inkluderingstags) // TODO: Navngiving på stilling?
        assertThat(stilling.prioriterteMålgrupper).isEqualTo(prioriterteMålgrupperTags)
        assertThat(stilling.tiltakEllerEllerVirkemidler).isEqualTo(tiltakVirkemiddelTags)
    }

    @Test
    fun `manglende tags på søketreff skal gi Stilling-objekt`() {
        val uuid = UUID.randomUUID().toString()
        val publiseringsdato = LocalDate.of(2019, 11, 20).atTime(10, 31, 32, 0)
        val klient = ElasticSearchKlientImpl(httpClientSøketreffUtenTags(uuid, publiseringsdato))

        val stilling = klient.hentStilling(uuid)

        assertThat(stilling).isNotNull()
        assertThat(stilling!!.uuid).isEqualTo(uuid)
        assertThat(stilling.publisert).isEqualTo(publiseringsdato)
        assertThat(stilling.tiltakEllerEllerVirkemidler).isEmpty()
        assertThat(stilling.prioriterteMålgrupper).isEmpty()
        assertThat(stilling.tiltakEllerEllerVirkemidler).isEmpty()
    }

    @Test
    fun `Ingen treff på dokumentuuid skal gi null`() {
        val uuid = UUID.randomUUID().toString()
        val klient = ElasticSearchKlientImpl(httpClientIngenTreff())

        val stilling = klient.hentStilling(uuid)

        assertThat(stilling).isNull()
    }

    private fun httpClientIngenTreff() =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("""
                        {"_index":"stilling_10","_type":"_doc","_id":"465869f2-a65f-4a8c-81d3-44763866d7e","found":false}
                    """.trimIndent())
                }
            }
        }

    private fun httpClientSøketreffUtenTags(uuid: String, publiseringsdato: LocalDateTime) =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond("""
                    {
                        "took": 1,
                        "timed_out": false,
                        "_shards": {
                            "total": 3,
                            "successful": 3,
                            "skipped": 0,
                            "failed": 0
                        },
                        "hits": {
                            "total": {
                                "value": 1,
                                "relation": "eq"
                            },
                            "max_score": 11.560985,
                            "hits": [
                                {
                                    "_index": "stilling_10",
                                    "_type": "_doc",
                                    "_id": "23976042-786e-4304-8e0b-21b74340cfe3",
                                    "_score": 11.560985,
                                    "_source": {
                                        "stilling": {
                                            "uuid": "$uuid",
                                            "created": "2019-01-03T12:02:26.262",
                                            "published":"$publiseringsdato",
                                            "properties": {}
                                        }
                                    }
                                }
                            ]
                        }
                    }
                    """.trimIndent())
                }
            }
        }

    private fun httpClientSøketreff(uuid: String,
                                    publiseringsdato: String,
                                    inkluderingsTags: List<InkluderingTag>,
                                    prioriterteMålgrupperTags: List<PrioriterteMålgrupperTag>,
                                    tiltakEllerVirkemiddelTags: List<TiltakEllerVirkemiddelTag>) =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        "/stilling/_doc/$uuid" -> {
                            respond(etElasticSearchSvarForEnStilling(uuid, publiseringsdato, inkluderingsTags, prioriterteMålgrupperTags, tiltakEllerVirkemiddelTags).trimIndent())
                        }
                        else -> error("Har ikke mocket kall mot ${request.url.fullPath}")
                    }
                }
            }
        }


}
