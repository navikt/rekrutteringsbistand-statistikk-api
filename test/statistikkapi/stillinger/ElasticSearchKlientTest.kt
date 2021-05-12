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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ElasticSearchKlientTest {

    // TODO: Kan stilling være uten publisertdato?
    // TODO: Ta en sjekk på datoformathåndtering

    @Test
    fun `skal kunne parse JSON fra stillingssøk-proxy til Stilling-objekt`() {
        val uuid = UUID.randomUUID().toString()
        val publiseringsdato = "2019-01-17T17:08:11"
        val inkluderingstags = InkluderingTag.values().toList()
        val prioriterteMålgrupperTags = PrioriterteMålgrupperTag.values().toList()
        val tiltakVirkemiddelTags = TiltakEllerVirkemiddelTag.values().toList()
        val klient = ElasticSearchKlient(httpClientSøketreff(uuid, publiseringsdato, inkluderingstags, prioriterteMålgrupperTags, tiltakVirkemiddelTags))

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
        val klient = ElasticSearchKlient(httpClientSøketreffUtenTags(uuid, publiseringsdato))

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
        val klient = ElasticSearchKlient(httpClientIngenTreff())

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
                                                        "title": "Tilkallingsvikarer Hjemmebaserte tjenester - Sykepleiere, helsefagarbeidere og assistenter (helsefagarbeider)",
                                                        "uuid": "$uuid",
                                                        "annonsenr": "216213",
                                                        "status": "INACTIVE",
                                                        "privacy": "SHOW_ALL",
                                                        "published": "$publiseringsdato",
                                                        "publishedByAdmin": "2019-01-03T12:00:13.185",
                                                        "expires": "2019-12-31T01:00:00",
                                                        "created": "2019-01-03T12:02:26.262",
                                                        "updated": "2020-01-01T00:00:02.181053",
                                                        "employer": {
                                                            "name": "MOERTUNET",
                                                            "publicName": "MOERTUNET",
                                                            "orgnr": "987907584",
                                                            "parentOrgnr": "874637602",
                                                            "orgform": "BEDR"
                                                        },
                                                        "categories": [
                                                            {
                                                                "styrkCode": "5321.02",
                                                                "name": "Helsefagarbeider"
                                                            }
                                                        ],
                                                        "source": "stillingsolr",
                                                        "medium": "NAV Servicesenter",
                                                        "businessName": "Ås kommune",
                                                        "locations": [
                                                            {
                                                                "address": null,
                                                                "postalCode": "1430",
                                                                "county": "VIKEN",
                                                                "countyCode": "30",
                                                                "municipal": "ÅS",
                                                                "municipalCode": "3021",
                                                                "latitue": null,
                                                                "longitude": null,
                                                                "country": "NORGE"
                                                            }
                                                        ],
                                                        "reference": "9950070",
                                                        "administration": {
                                                            "status": "DONE",
                                                            "remarks": [],
                                                            "comments": "",
                                                            "reportee": "System",
                                                            "navIdent": ""
                                                        },
                                                        "properties": {
                                                            "tags": 
                                                                ${listOf(
                                                                    lagStringlisteInkluderingsTags(inkluderingsTags),
                                                                    lagStringlistePrioriterteMålgrupperTags(prioriterteMålgrupperTags),
                                                                    lagStringlisteTiltakEllerVirkemiddelTags(tiltakEllerVirkemiddelTags)
                                                                ).flatten()},
                                                            "searchtags": [
                                                                {
                                                                    "label": "Helsefagarbeider",
                                                                    "score": 0.9595941
                                                                },
                                                                {
                                                                    "label": "Lærling helsefagarbeider",
                                                                    "score": 0.67662627
                                                                },
                                                                {
                                                                    "label": "Demenssykepleier",
                                                                    "score": 0.66041595
                                                                },
                                                                {
                                                                    "label": "Pleieassistent",
                                                                    "score": 0.5842665
                                                                },
                                                                {
                                                                    "label": "Junior Nursing Assistant",
                                                                    "score": 0.5603502
                                                                }
                                                            ],
                                                            "engagementtype": "Annet",
                                                            "classification_styrk08_score": 0.96505976,
                                                            "location": "Ås",
                                                            "jobtitle": "Helsefagarbeider",
                                                            "workhours": "Turnus",
                                                            "employerdescription": "<p>.</p>\n",
                                                            "classification_input_source": "jobtitle",
                                                            "adtext": "<p><strong>Kvalifikasjoner:</strong>\nSykepleiere og helsefagarbeidere må ha godkjent norsk autorisasjon<br />\nFørerkort klasse B<br />\nGode norskkunnskaper, muntlig og skriftlig</p>\n<p><strong>Opplysninger om arbeidssted og stilling:</strong>\nHjemmebaserte tjenester består av distrikt nord og distrikt sør, samt en avdeling for brukerstyrte personlige assistenter, kreftkoordinator og fagsykepleier. Vi arbeider for at brukere og pasienter skal motta tilbud hvor trygghet, respekt, verdighet og kompetanse oppleves gjennom tjenestene vi yter.</p>\n<p>Hjemmebaserte tjernester er alltid interessert i å komme i kontakt med dyktige og engasjerte sykepleiere, helsefagarbeidere og assistenter. Selv om vi har et stabilt personale, vil det likevel være et jevnt behov for nye vikarer som kan tilkalles ved behov.</p>\n<p>Din søknad blir liggende i vår database frem til 31.12.2018, dersom du fortsatt er interessert i å være tilkallingsvikar må du søke på nytt i 2020. Din søknad vil bli behandlet fortløpende ved behov og du vil bli kontaktet dersom det skulle bli aktuelt.<br />\nDu ønskes også velkommen til å søke på utlyste stillinger via vår hjemmeside - <a href=\"http://\" rel=\"nofollow\">www.as.kommune.no/ledige-stillinger.no</a></p>\n<ul><li></li></ul>\n<p><strong>Vi ønsker deg som:</strong></p>\n<ul><li>Liker å arbeide med mennesker</li><li>Har erfaring fra tilsvarende arbeid</li><li>Har gode kommunikasjons- og samarbeidsevner</li><li>Kan arbeide selvstendig, strukturert og målrettet</li><li>Har godt humør og bidrar til et godt arbeidsmiljø</li></ul>\n<p>Personlig egnethet vil bli vektlagt</p>\n<p><strong>Hos oss får du:</strong></p>\n<ul><li>Muligheten til å arbeide i en kommune med stort samfunnsansvar</li><li>Faglig utvikling</li><li>Lønn i henhold til tariffavtale og Ås kommunes lokale lønnspolitikk, bl.a. med tillegg for utvidet kompetanse og ansvar for enkelte stillinger</li><li>Gode forsikrings- og pensjonsordninger, blant annet gruppelivs- og fritidsforsikring</li></ul>\n<p><strong>Den vi ansetter, må legge frem tilfredsstillende politiattest før tiltredelse.</strong></p>\n<p>Ås kommune er en IA- virksomhet og ønsker å gjenspeile mangfoldet i befolkningen. Alle kvalifiserte kandidater oppfordres til å søke stilling uavhengig av kjønn, alder, funksjonsevne eller etnisk bakgrunn. I henhold til offentleglova kan søknaden offentliggjøres selv om du har bedt om å ikke bli oppført på offentlig søkerliste. Du vil i så fall bli varslet.</p>\n<p><strong>Ås kommune ønsker ikke tilbud fra rekrutteringsfirma eller annonsører.</strong> <a href=\"https://candidate.hr-manager.net/ApplicationInit.aspx?cid&#61;1049&amp;ProjectId&#61;144780&amp;DepartmentId&#61;19067&amp;MediaId&#61;4611&amp;SkipAdvertisement&#61;true\" rel=\"nofollow\">Søk på stillingen</a></p>\n",
                                                            "extent": "Heltid",
                                                            "sector": "Ikke oppgitt",
                                                            "employer": "Ås kommune",
                                                            "applicationdue": "2019-12-31T01:00"
                                                        }
                                                    }
                                                }
                                            }
                                        ]
                                    }
                                }
                            """.trimIndent())
                        }
                        else -> error("Har ikke mocket kall mot ${request.url.fullPath}")
                    }
                }
            }
        }

    private fun lagStringlisteInkluderingsTags(tags: List<InkluderingTag>) = tags.map {""" "INKLUDERING__${it.name}" """}
    private fun lagStringlistePrioriterteMålgrupperTags(tags: List<PrioriterteMålgrupperTag>) = tags.map {""" "PRIORITERT_MÅLGRUPPE__${it.name}" """}
    private fun lagStringlisteTiltakEllerVirkemiddelTags(tagEllers: List<TiltakEllerVirkemiddelTag>) = tagEllers.map {""" "TILTAK_ELLER_VIRKEMIDDEL__${it.name}" """}
}
