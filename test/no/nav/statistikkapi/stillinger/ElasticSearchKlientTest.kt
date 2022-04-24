package no.nav.statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import org.junit.Test
import no.nav.statistikkapi.etElasticSearchSvarForEnStilling
import no.nav.statistikkapi.BearerToken
import no.nav.statistikkapi.etElasticSearchSvarForEnStillingMedTagsogStatligInkluderingsdugnad
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
        val httpKlientMedSøketreff = httpClientSøketreff(uuid, publiseringsdato, inkluderingstags, prioriterteMålgrupperTags, tiltakVirkemiddelTags)
        val klient = ElasticSearchKlientImpl(httpKlientMedSøketreff, ::tokenProvider)
        val stilling = klient.hentStilling(uuid)

        assertThat(stilling).isNotNull()
        assertThat(stilling!!.uuid).isEqualTo(uuid)
        assertThat(stilling.publisert.toString()).isEqualTo(publiseringsdato)
        assertThat(stilling.inkluderingsmuligheter).isEqualTo(inkluderingstags)
        assertThat(stilling.prioriterteMålgrupper).isEqualTo(prioriterteMålgrupperTags)
        assertThat(stilling.tiltakEllerEllerVirkemidler).isEqualTo(tiltakVirkemiddelTags)
    }

    @Test
    fun `skal kunne parse JSON med uhåndtert tag statlig inkluderingsdugnad fra stillingssøk-proxy til Stilling-objekt`() {
        val httpKlientMedSøketreffInkludertTagForStatligInkluderingsdugnad = httpClientSøketreffMedStatligInkluderingsdugnad()
        val klient = ElasticSearchKlientImpl(httpKlientMedSøketreffInkludertTagForStatligInkluderingsdugnad, ::tokenProvider)
        val stilling = klient.hentStilling("UUID")

        assertThat(stilling).isNotNull()
    }

    @Test
    fun `manglende tags på søketreff skal gi Stilling-objekt`() {
        val uuid = UUID.randomUUID().toString()
        val publiseringsdato = LocalDate.of(2019, 11, 20).atTime(10, 31, 32, 0)
        val httpKlientSøketreffUtenTags = httpClientSøketreffUtenTags(uuid, publiseringsdato)
        val klient = ElasticSearchKlientImpl(httpKlientSøketreffUtenTags, ::tokenProvider)

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
        val klient = ElasticSearchKlientImpl(httpClientIngenTreff(), ::tokenProvider)

        val stilling = klient.hentStilling(uuid)

        assertThat(stilling).isNull()
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
                        "_index": "stilling_11",
                        "_type": "_doc",
                        "_id": "207d78cf-c1b2-4a73-aa60-b9271a3d8dbd",
                        "_version": 2,
                        "_seq_no": 318052,
                        "_primary_term": 1,
                        "found": true,
                        "_source": {
                            "stilling": {
                                "uuid": "$uuid",
                                "created": "2019-01-03T12:02:26.262",
                                "published":"$publiseringsdato",
                                "properties": {}
                            },
                            "stillingsinfo": {
                                "stillingskategori": "STILLING"
                            }
                        }
                    }
                    """.trimIndent())
                }
            }
        }

    private fun httpClientSøketreffMedStatligInkluderingsdugnad() =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(etElasticSearchSvarForEnStillingMedTagsogStatligInkluderingsdugnad().trimIndent())
                }
            }
        }

    private fun tokenProvider() = BearerToken("a", LocalDateTime.now().plusSeconds(10))
}
