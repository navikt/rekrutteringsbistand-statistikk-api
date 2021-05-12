package statistikkapi

import statistikkapi.kandidatutfall.OpprettKandidatutfall
import statistikkapi.kandidatutfall.Utfall
import statistikkapi.stillinger.ElasticSearchStilling
import statistikkapi.stillinger.InkluderingTag
import statistikkapi.stillinger.PrioriterteMålgrupperTag
import statistikkapi.stillinger.TiltakEllerVirkemiddelTag
import java.time.LocalDate

const val enNavIdent = "X123456"
const val enAnnenNavIdent = "Y654321"

const val etKontor1 = "1234"
const val etKontor2 = "2000"

data class OpprettKandidatutfallMedFærreFelt(
    val aktørId: String,
    val utfall: Utfall,
    val navIdent: String,
    val navKontor: String,
    val kandidatlisteId: String,
    val stillingsId: String,
    val harHullICv: Boolean?,
    val alder: Int?
)

val etKandidatutfall = OpprettKandidatutfall(
    aktørId = "10000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = "24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2",
    synligKandidat = true,
    harHullICv = true,
    alder = 54,
    tilretteleggingsbehov = listOf("permittert", "arbeidstid")
)

val etKandidatutfallMedUkjentHullICv = OpprettKandidatutfall(
    aktørId = "80000254879658",
    utfall = Utfall.PRESENTERT,
    navIdent = enAnnenNavIdent,
    navKontor = etKontor1,
    kandidatlisteId = "385c74d1-0d14-48d7-9a9b-b219beff22c8",
    stillingsId = "24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2",
    synligKandidat = true,
    harHullICv = null,
    alder = null,
    tilretteleggingsbehov = emptyList()
)

fun enElasticSearchStilling() = ElasticSearchStilling(
    uuid = "24f0074a-a99a-4b9a-aeaa-860fe6a7dbe2",
    opprettet = LocalDate.of(2021, 3, 3).atStartOfDay(),
    publisert = LocalDate.of(2021, 3, 3).atStartOfDay(),
    inkluderingsmuligheter = listOf(InkluderingTag.FYSISK),
    prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.HULL_I_CV, PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS),
    tiltakEllerEllerVirkemidler = listOf(TiltakEllerVirkemiddelTag.LÆRLINGPLASS)
)

fun etElasticSearchSvarForEnStilling(
    uuid: String,
    publiseringsdato: String,
    inkluderingsTags: List<InkluderingTag>,
    prioriterteMålgrupperTags: List<PrioriterteMålgrupperTag>,
    tiltakEllerVirkemiddelTags: List<TiltakEllerVirkemiddelTag>
) = """
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
                                    ${
                                        listOf(
                                            lagStringlisteInkluderingsTags(inkluderingsTags),
                                            lagStringlistePrioriterteMålgrupperTags(prioriterteMålgrupperTags),
                                            lagStringlisteTiltakEllerVirkemiddelTags(tiltakEllerVirkemiddelTags)
                                        ).flatten()
                                    },
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
    """

private fun lagStringlisteInkluderingsTags(tags: List<InkluderingTag>) = tags.map {""" "INKLUDERING__${it.name}" """}
private fun lagStringlistePrioriterteMålgrupperTags(tags: List<PrioriterteMålgrupperTag>) = tags.map {""" "PRIORITERT_MÅLGRUPPE__${it.name}" """}
private fun lagStringlisteTiltakEllerVirkemiddelTags(tagEllers: List<TiltakEllerVirkemiddelTag>) = tagEllers.map {""" "TILTAK_ELLER_VIRKEMIDDEL__${it.name}" """}
