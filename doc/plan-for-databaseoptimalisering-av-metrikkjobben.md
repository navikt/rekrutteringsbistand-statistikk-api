# Plan for indekser til metrikkjobben

Status: Foreløpig plan. Brukeren har avgrenset denne omgangen til databasetiltak uten SQL-omskriving. Tre representative produksjonsplaner er mottatt. Indekssett og migrasjonsstrategi må fortsatt verifiseres. Ingen repository-filer er endret.

## Mål og avgrensning

Reduser databasearbeidet fra `MetrikkJobb` uten å endre statistikkresultater, hendelsesbehandling eller oppdateringsintervall. Start med målrettede indekser og behold eksisterende SQL. Vurder omskriving av spørringer separat dersom målinger viser at indekser ikke er tilstrekkelig.

Dette er en avgrenset optimalisering av eksisterende databasebruk (komprimert løp). Ingen nye dataflyter, mottakere, API-kontrakter eller tilgangsendringer er planlagt. Ingen endring i Nais, CI/CD, avhengigheter eller tilkoblingspool inngår.

Vi velger indekser etter spørringsplaner og målt ressursbruk, ikke antall sekvensielle skann alene. Indekser gir ekstra lagringsplass og arbeid ved skriving, og brede historiske aggregater kan fortsatt være raskest med sekvensielle skann. Materialiserte aggregater og endret datamodell er utenfor første leveranse.

## Nåtilstand fra kildekoden

- `src/no/nav/statistikkapi/metrikker/MetrikkJobb.kt` kaller 21 repository-metoder ved initialisering og ved ordinær oppdatering. Jobben kjører sekvensielt med 60 minutters pause etter fullført kjøring. Seks månedsaggregater kjøres også fra `initierNyeMåneder`.
- `src/no/nav/statistikkapi/application.kt:173–198` starter jobben i en bakgrunnstråd og logger samlet oppstartstid. `MetrikkJobb` logger samlet tid ved ordinær kjøring, ikke tid per spørring.
- Migrasjonene V1–V18 inneholder ingen `CREATE INDEX`. `kandidatutfall`, `kandidatliste` og `visning_kontaktinfo` har bare primærnøkkelindeksen på `id` i det versjonerte skjemaet. `stilling` har primærnøkkel på `uuid`. Faktisk skjema i produksjon er ikke kontrollert og kan ha manuelt opprettede indekser.
- `KandidatlisteRepository` finner nyeste kandidatutfall med `max(id)` gruppert på `(aktorid, kandidatlisteid)`. Dette gjentas i fem metrikkspørringer.
- Tre summeringer av antall stillinger bruker vindusfunksjonen `first_value(tidspunkt_for_hendelsen)` per `kandidatliste_id`, gruppering og en join tilbake til hendelsestabellen.
- Tomme kandidatlister bruker `max(id)` per `kandidatliste_id`. Dette er en annen definisjon av «siste» enn høyeste hendelsestidspunkt.
- Kontaktinfometrikkene kobler `kandidatliste.stillings_id`, `kandidatutfall.stillingsid` og `visning_kontaktinfo.stilling_id::text`. Kandidatutfall kobles også på `aktorid`.
- De to globale utfallstellingene bruker historiske `DISTINCT (aktorid, kandidatlisteid)` med filter på utfall. De velger ikke bare siste utfall.
- Månedsspørringene leser fra mars 2023 og fremover. Et slikt datofilter blir mindre selektivt over tid.
- `Database.kt` kjører Flyway under oppstart, med standard transaksjonshåndtering. Hikari-poolen har maksimalt to forbindelser. Økt parallellisering av metrikkspørringene inngår ikke.
- `TestDatabase.kt` bruker PostgreSQL `14.4-alpine` og de samme Flyway-migrasjonene. Produksjonsversjonen er ikke verifisert.

## Indekskandidater

### Mottatte produksjonsmålinger

Brukeren delte følgende tabellstatistikk 19. november 2026. Radantallene er estimater. Databasens `stats_reset` er `2026-10-27T07:58:40.158201Z`, omtrent 23 døgn før uttrekket. Enkelte tabelltellere kan likevel ha blitt nullstilt separat.

Produksjon rapporterer `PostgreSQL 18.6 on x86_64-pc-linux-gnu, compiled by Debian clang version 12.0.1, 64-bit`. Ytelsesmålinger og kontroll av spørringsplaner bør derfor bruke PostgreSQL 18, ikke testoppsettets PostgreSQL 14.4. En eventuell endring av test-image må avklares separat; ingen oppgradering inngår automatisk.

| Tabell | Estimerte rader | Tabellstørrelse | Indeksstørrelse | seq_scan | seq_tup_read | idx_scan |
|--------|----------------|----------------|-----------------|----------|--------------|----------|
| kandidatliste | 706 705 | 138 MB | 22 MB | 124 614 | 36 974 796 026 | 58 074 187 |
| kandidatutfall | 583 905 | 114 MB | 19 MB | 1 197 717 | 240 268 438 480 | 10 253 737 528 |
| visning_kontaktinfo | 107 879 | 8224 kB | 3208 kB | 18 332 | 1 950 382 732 | 0 |
| stilling | 92 130 | 7480 kB | 7072 kB | 115 678 | 10 558 991 532 | 370 473 |

| Tabell | last_analyze | last_autoanalyze |
|--------|--------------|------------------|
| kandidatliste | 2026-10-27T10:47:34.220079Z | 2026-10-27T10:01:17.128751Z |
| kandidatutfall | 2026-10-27T10:47:33.448609Z | 2026-10-27T10:01:11.646498Z |
| visning_kontaktinfo | 2026-10-27T10:47:34.451338Z | 2026-10-27T10:01:17.377013Z |
| stilling | 2026-10-27T10:47:33.608263Z | 2026-10-27T10:01:11.837145Z |

`kandidatutfall` har klart størst kumulativt antall rader lest ved sekvensielle skann, rundt 240 milliarder. Prioriter den første EXPLAIN-spørringen for å undersøke siste-utfall-grupperingen og joinene. Tallene identifiserer ikke hvilke spørringer som står for lesingen, og `seq_tup_read` er ikke fysisk disk-I/O.

De rundt 10,3 milliarder indeksskannene på `kandidatutfall` må også undersøkes. Gjentatte primærnøkkeloppslag i nested loops kan gi høye tellere uten at det finnes nyttige sekundærindekser. Tellerne dokumenterer verken hvilke indekser som finnes eller at en bestemt joinstrategi brukes.

`visning_kontaktinfo` har ingen registrerte indeksskann i måleperioden. Tabellen er samtidig mindre enn de to historikktabellene, så et sekvensielt skann kan være et rimelig valg. Siste registrerte statistikkoppdatering er 27. oktober for alle fire tabeller. Dette beviser ikke utdaterte estimater uten kunnskap om endringer siden da og avvik i EXPLAIN.

Brukeren delte også indeksdefinisjonene fra produksjon:

```sql
CREATE UNIQUE INDEX kandidatliste_pkey ON public.kandidatliste USING btree (id);
CREATE UNIQUE INDEX kandidatutfall_pkey ON public.kandidatutfall USING btree (id);
CREATE UNIQUE INDEX stilling_pkey ON public.stilling USING btree (uuid);
CREATE UNIQUE INDEX visning_kontaktinfo_pkey ON public.visning_kontaktinfo USING btree (id);
```

Produksjonsuttrekket bekrefter at de fire tabellene bare har primærnøkkelindekser, i samsvar med migrasjonene. Ingen eksisterende sekundærindeks overlapper indekskandidatene nedenfor. Indeksuttrekket viser definisjoner, ikke `pg_index.indisvalid`.

Alle registrerte indeksskann på `kandidatutfall` gjelder dermed primærnøkkelindeksen. Det styrker behovet for å undersøke gjentatte ID-oppslag i spørringsplanene, men dokumenterer ikke hvilken spørring eller planoperasjon som står bak.

I PostgreSQL 18 teller `idx_scan` primitive indekssøk, og én planoperasjon kan gjøre flere slike søk. Ikke likestill telleren med antall SQL-kall. Spørringsplanene må vise hvor søkene skjer.

### Første målte spørringsplan

Brukeren delte `EXPLAIN ANALYZE` for `hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()`.

| Operasjon | Faktisk observasjon |
|-----------|--------------------|
| Hele spørringen | Execution Time: 10 677,499 ms. Shared hit: 2 180 283. Ingen shared read eller temp-I/O rapportert. |
| Sekvensielt skann av kandidatutfall | 583 905 rader, 136,734 ms, 14 618 buffer hits. |
| Gruppering per kandidat/liste | 536 758 grupper, estimert 101 761. HashAggregate ferdig ved 797,550 ms. |
| Deduplisering av beregnede max-ID-er | 536 758 rader, estimert 200. Ytre HashAggregate ferdig ved 1 252,949 ms. |
| Primærnøkkeloppslag etter max-ID | 536 758 Index Searches og loops, 2 147 032 buffer hits. Nested Loop ferdig ved 3 432,353 ms, med 532 151 godkjente utfall. |
| Sekvensielt skann av kandidatliste | 682 961 godkjente rader, 23 749 forkastet, 125,521 ms. Filteret godkjenner omtrent 96,6 % av tabellen. |
| Join fra kandidatliste til stilling | 607 085 rader, ferdig ved 348,028 ms. |
| Join mellom utfall og kandidatlistehistorikk | 7 411 268 rader, estimert 413 363 (omtrent 18 ganger for lavt). |
| Sluttresultat | Én rad etter `count(DISTINCT kandidatliste_id)`. |
| Sortering og hash | Rapportert quicksort i minne, 24 577 kB per sortering. Hash-operasjonene har én batch. |
| Innstillinger | `work_mem = '64MB'`, `effective_cache_size = '2015496kB'`. Planning Time: 2,819 ms. |

Dette endrer prioriteringen: Sekvensielle skann er ikke hovedkostnaden i denne kjøringen. Skannene av kandidatutfall og kandidatliste bruker rundt 262 ms til sammen av en total på 10,68 sekunder. Primærnøkkeloppslagene står for omtrent 98,5 % av de rapporterte buffer hits. Det store joinresultatet må deretter dedupliseres. Alle disse bufferlesingene er cache-treff, ikke dokumenterte fysiske disklesinger.

536 758 grupper fra 583 905 utfallsrader betyr at «siste utfall» beholder omtrent 92 % av radene. En indeks på grupperingsnøkkelen kan redusere arbeid, men vil ikke alene fjerne de 536 758 etterfølgende ID-oppslagene i eksisterende SQL. Et partial-filter på direktemeldte, opprettede kandidatlister blir heller ikke selektivt i dette datasettet.

Sort-noden på kandidatlistesiden rapporterer 7 415 959 returnerte rader selv om barnet leverer 607 085. Dette er forenlig med gjentatt avlesing av like nøkler i Merge Join, ikke like mange unike kandidatlistemeldinger eller et nytt tabellskann. Ikke summer inklusive nodetider som om de var separate kostnader.

Første spørring gir grunn til å vurdere en separat, karakterisert SQL-omskriving med eksistenssjekk eller tidlig deduplisering for akkurat denne tellingen, slik at kandidatlistens hendelseshistorikk ikke multipliseres med alle matchende kandidater før `DISTINCT`. Dette er et alternativ som krever godkjenning, ikke en besluttet endring. Bevar «siste = høyeste id» og velg siste utfall før utfallsfilteret. Ikke overfør dedupliseringen ukritisk til månedsmetodene, som bruker flere kolonner i sin distinkthet.

Estimatavvikene skal undersøkes før valg av tiltak. Vurder i testmiljø om fersk statistikk og multikolonnestatistikk for antall distinkte `(aktorid, kandidatlisteid)` forbedrer gruppeestimatet. Det løser ikke nødvendigvis estimatet på 200 for deduplisering av `max(id)` eller estimatet for joinen.

### Andre mottatte spørringsplan: kontaktinfovisninger

Den andre innsendte planen gjelder kontaktinfo per måned (`hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()`), altså den tredje EXPLAIN-spørringen i kjøreveiledningen. Planen for siste hendelsestidspunkt per kandidatliste gjenstår.

| Operasjon | Faktisk observasjon |
|-----------|--------------------|
| Hele spørringen | Execution Time: 19 461,288 ms, 45 månedsrader. Shared hit: 35 426. Ingen shared read rapportert. |
| Parallellitet | To workers og hovedprosessen deltar, vist som `loops=3`. |
| Kandidatlistefilter | Omtrent 692 572 godkjente rader totalt, rundt 98 % av tabellen. Parallel Seq Scan rapporterer 173,267 ms per deltaker i gjennomsnitt. |
| Kandidatutfall | Parallel Seq Scan rapporterer 112,604 ms per deltaker i gjennomsnitt. Sorterer deretter på `(stillingsid, aktorid)` i minne. |
| Kontaktinfovisninger | Hele tabellen med 107 884 rader skannes og sorteres separat av hver av de tre deltakerne. Skann rapporterer 47,704 ms per deltaker. |
| Join kontaktinfo/utfall | 40 630 rader per deltaker i gjennomsnitt, omtrent 121 890 totalt. Estimert fem per deltaker, omtrent 8 126 ganger for lavt. |
| Join mot kandidatlistehistorikk | 4 295 272 rader totalt før sluttaggregering. Gather Merge estimerer 331 rader, omtrent 13 000 ganger for lavt. |
| Sluttsortering | `external merge` hos alle tre deltakere. Rapportert diskplass 41 608, 49 216 og 48 776 kB, samlet omtrent 136 MiB. |
| Midlertidig I/O | 17 450 blokker lest og 17 453 skrevet. Rapportert temp read-tid 93,843 ms og write-tid 389,231 ms. |
| Innstillinger | `work_mem = '64MB'`, `effective_cache_size = '2015496kB'`. Planning Time: 1,969 ms. |

Også her dominerer ikke selve tabellskannene. Joinen mellom kontaktinfo og kandidatutfall blir multiplisert omtrent 35 ganger videre av kandidatlistens hendelseshistorikk. Sluttresultatet trenger bare distinkte stillinger per måned, men sorterer over 4,29 millioner rader først. De svært lave estimatene forklarer hvorfor forventet arbeidsmengde avviker kraftig fra den faktiske.

Indeksen `kandidatutfall (stillingsid, aktorid)` er fortsatt relevant å prøve, særlig for sortering og oppslag i den første joinen. Den fjerner ikke radmultiplikasjonen. En vanlig indeks på `kandidatliste (stilling_opprettet_tidspunkt)` er lite lovende her siden datofilteret beholder omtrent 98 % av tabellen. Ikke bestill en uttrykksindeks på måned for å løse sorteringen: det er joinresultatet som må sorteres, og kildeindeks alene garanterer ikke riktig rekkefølge gjennom joinene.

Ved godkjent utvidelse av omfanget er en konkret omskrivingskandidat å la hver kontaktinfovisning passere to `EXISTS`-sjekker: én for en kandidatlisterad med samme stilling og dato etter grensen, og én for kandidatutfall med samme stilling og aktør. Behold måned og distinkt stilling som i originalen. Denne metoden skal fortsatt se etter ethvert matchende historisk utfall, uten siste-utfall-filter eller filter på utfallstype. Behold eksisterende tekstsammenligning av stillings-ID, datogrense og tidszoneatferd.

Målgruppevariantene trenger egne tester før samme mønster eventuelt brukes der: målgruppevilkårene må gjelde samme kandidatutfallsrad som stilling og aktør, og utfallsfilteret er ikke likt i alle kontaktinfometodene.

Ikke øk global `work_mem` som første tiltak. Diskspill er dokumentert, men å redusere mellomresultatet angriper både sorteringsarbeidet og radbehandlingen. Temp-I/O-tidene dokumenterer ikke alene hvor hele kjøretiden brukes. Ikke summer inklusive nodetider eller gjennomsnittlige parallelltider som sekvensielle kostnader.

### Tredje mottatte spørringsplan: siste hendelsestidspunkt

Planen gjelder `hentAntallStillingerForStillingsannonserMedKandidatliste()`, med `first_value(tidspunkt_for_hendelsen)`.

| Operasjon | Faktisk observasjon |
|-----------|--------------------|
| Hele spørringen | Execution Time: 6 082,152 ms. Shared hit: 35 415. Ingen shared read eller temp-I/O rapportert. |
| Filtrert tabellskann | 705 471 godkjente rader, 1 241 forkastet, 144,961 ms. Filteret beholder omtrent 99,8 % av tabellen. |
| Ufiltrert tabellskann | 706 712 rader, 163,075 ms. |
| HashAggregate på liste og tidspunkt | 705 471 grupper. Grupperingen reduserer ikke radantallet i denne kjøringen. |
| Sortering før vindusfunksjon | 705 471 rader, quicksort, 61 966 kB. Sort-nodens første resultat kommer rundt 3,30 sekunder etter barnets siste resultat, som peker på betydelig sorteringsarbeid i denne kjøringen. |
| WindowAgg | 705 471 rader, gjentar første tidspunkt på hver historikkrad per liste. |
| Join tilbake på liste og valgt tidspunkt | 705 471 rader mot estimerte 28, omtrent 25 200 ganger for lavt. |
| Sluttsortering og Unique | Sorterer 705 471 rader i minne og returnerer 42 237 distinkte `(kandidatliste_id, antall_stillinger)`. |
| Innstillinger | `work_mem = '64MB'`, `effective_cache_size = '2015496kB'`. Planning Time: 0,974 ms. |

De to tabellskannene bruker samlet omtrent 308 ms. Hovedarbeidet ligger i gruppering, sortering, vindusfunksjon og gjentatte mellomrader, ikke i selve skannene. Siste-rad-resultatet er omtrent 16,7 ganger mindre enn joinresultatet før `Unique`.

Indeksen `(kandidatliste_id, tidspunkt_for_hendelsen DESC)` er fortsatt en kandidat for sorteringsrekkefølge og join. Den må prøves med eksisterende SQL: en HashAggregate kan bryte indeksrekkefølgen, slik at indeksen ikke automatisk fjerner sorteringen.

Hvis SQL-omskriving godkjennes, prøv å erstatte `first_value` over alle hendelser med én `max(tidspunkt_for_hendelsen)` per kandidatliste i den filtrerte delspørringen. Join dette tilbake til den ufiltrerte tabellen på liste og tidspunkt, og behold sluttresultatets `DISTINCT (kandidatliste_id, antall_stillinger)`. Dette reduserer gjentatte tidsstempler uten å velge en vilkårlig rad ved like tidspunkter.

Bevar filtreringen separat for alle tre variantene: beregn høyeste tidspunkt blant radene med opprettet tidspunkt og eventuelt riktig `er_direktemeldt`, men behold originalens ufiltrerte ytre join. Ikke bruk `DISTINCT ON ... LIMIT 1` eller velg høyeste `id` som erstatning. Flere rader med samme siste tidspunkt kan ha forskjellige `antall_stillinger`, og originalen beholder alle distinkte liste/antall-par. Tidspunktkolonnen er `NOT NULL`, men `kandidatliste_id` kan være null. Behold vanlig likhetsjoin slik at null-lister behandles som før.

### Samlet prioritering etter tre planer

De tre separate kjøringene tok henholdsvis 10,68, 19,46 og 6,08 sekunder. Summen på omtrent 36,22 sekunder er ikke en målt total for hele metrikkjobben.

Alle tre planer viser mye arbeid etter tabellskannene. Anbefalingen er derfor å utvide første leveranse til karakteriserte SQL-omskrivinger sammen med et målt, lite indekssett, dersom brukeren godkjenner det. Ikke opprett alle indekskandidatene på grunnlag av skann-tellerne. Mål original SQL med indeks alene og omskrevet SQL med og uten indeks, slik at tiltakets effekt kan skilles.

Rekkefølgen for eksperimentene er kontaktinfo per måned (størst målt tid og disksortering), siste-utfall-tellingen (mange ID-oppslag og historikkmultiplikasjon), deretter summering fra siste hendelsestidspunkt (sortering og gjentatte tidsstempler). Undersøk tilsvarende varianter før felles logikk eventuelt gjenbrukes.

Alle tre representative planer er nå mottatt. Brukeren har deretter avgrenset denne omgangen til databasetiltak uten endring av applikasjonens SQL. Omskrivingsforslagene ovenfor er bakgrunn for en senere oppgave, ikke del av leveransen. Ingen produksjonsinngrep er godkjent gjennom deling av måleresultatene.

### Anbefalt databaseløp uten SQL-omskriving

1. Etabler baseline og oppdater planleggerstatistikk i godkjent miljø. Undersøk endringer siden siste ANALYZE. Prøv målrettet `ANALYZE` og multikolonnestatistikk av typen `ndistinct` for `(aktorid, kandidatlisteid)` på kandidatutfall. Dette kan forbedre det observerte gruppeestimatet på 101 761 mot faktisk 536 758. Det er ikke en lovnad om å rette estimatet på 200 for `max(id)`-deduplisering eller feilestimater gjennom joins. Unngå å øke statistikkmålet globalt.
2. Prøv tre indekskandidater enkeltvis på PostgreSQL 18 med uendret SQL: `kandidatliste (kandidatliste_id, tidspunkt_for_hendelsen DESC)`, `kandidatutfall (stillingsid, aktorid)` og `kandidatutfall (aktorid, kandidatlisteid, id DESC)`. Den første retter seg mot sortering/join i siste-hendelsesmetodene, den andre mot sorteringen/joinen i kontaktinfometodene og den tredje mot siste-utfall-gruppering og eksisterende punktoppslag. Ingen av dem har dokumentert gevinst ennå. Full indeks foretrekkes som første eksperiment fremfor et partial-filter som beholder nesten hele tabellen.
3. Prøv `kandidatliste (stillings_id)` som en etterfølgende kandidat dersom de nye planene viser et fordelaktig oppslagsmønster. Ikke prioriter den bare fordi kolonnen brukes i join: de observerte brede hash-joinene kan være rimelige. Prøv eventuell INCLUDE-utvidelse først når planen viser hvilke heap-oppslag som faktisk kan unngås.
4. Ikke øk global `work_mem`, parallellitet eller serverstørrelse i første leveranse. Kontaktinfospørringen har dokumentert diskspill, men de andre to har ikke det. En kontrollert sesjonstest med høyere `work_mem` kan måle potensialet senere, med avtalt minnebudsjett og timeout. Minnegrensen gjelder per operasjon og deltaker, ikke per spørring, og en global økning kan ramme samtidige kall.
5. Flytt bare tiltak med gjentakbar målt gevinst videre til additive migrasjoner og kontrollert produksjonsdeploy. Verifiser både uendret resultat og samlet databasebelastning. Behold brede sekvensielle skann når de er billigst. Ingen REINDEX, VACUUM FULL eller databaseomstart anbefales ut fra disse planene.

Forventningsgrense: Databaseendringer kan bedre planer, sortering og datatilgang, men kan ikke forventes å fjerne historikkmultiplikasjonen i eksisterende SQL. Mål gevinsten før det settes et konkret ytelsesmål.

Dette er hypoteser som skal prøves enkeltvis. Tabellen er ikke en bestilling på å opprette alle indeksene.

| Prioritet | Tabell og indeksnøkkel | Spørringer og begrunnelse | Forbehold |
|-----------|-----------------------|--------------------------|-----------|
| 1 | `kandidatutfall (aktorid, kandidatlisteid, id DESC)` | Dekker grupperingsnøkkel og ID for gjentatte `max(id)`-aggregater. Treffer også eksisterende oppslag etter siste utfall for én kandidat og liste. | PostgreSQL må fremdeles lese alle relevante indeksrader ved global gruppering. Ingen garanti for å fjerne sekvensielle skann eller gi størst gevinst. |
| 1 | `kandidatliste (kandidatliste_id, tidspunkt_for_hendelsen DESC)` med eventuell `INCLUDE (antall_stillinger)` | Treffer sortering/gruppering og join tilbake på liste og hendelsestidspunkt i de tre stillingssummeringene. Første kolonne støtter også joins og listeoppslag. | Et eventuelt partial-predikat på opprettet tidspunkt passer ikke nødvendigvis den ufiltrerte ytre joinen. Prøv først full indeks og mål før flere varianter legges til. |
| 1 | `kandidatliste (stillings_id)` | Join fra kontaktinfovisninger til kandidatliste mangler en indeks i skjemaet. | Brede joins kan likevel velge hash join og sekvensielle skann. Mål før eventuell `INCLUDE` eller partial-indeks. |
| 1 | `kandidatutfall (stillingsid, aktorid)` | Treffer den andre siden av kontaktinfojoinene. Første kandidatindeks dekker ikke denne nøkkelen. | Utvidelse med målgruppekolonner øker størrelsen. Ikke legg dem til uten målt gevinst. |
| 2 | `visning_kontaktinfo ("aktør_id", stilling_id, tidspunkt)` | Kandidat for dedupliseringsoppslag og gruppering på kandidat og stilling. | Cast til tekst i metrikkjoinene betyr at en vanlig UUID-indeks ikke automatisk støtter oppslag på tekstuttrykket. Kontroller faktisk joinretning og plan før valg av indeksrekkefølge eller uttrykksindeks. |
| 2 | `kandidatliste (kandidatliste_id, id DESC)` | Kan støtte `max(id)` per liste for tomme kandidatlister. | Tidspunktindeksen dekker ikke `id`-rekkefølgen. Vurder tillegg bare hvis denne spørringen står for vesentlig kostnad. |
| 2 | `kandidatutfall (utfall, aktorid, kandidatlisteid)`, alternativt partial-indeks på `(aktorid, kandidatlisteid)` for relevante utfall | Kandidater for globale historiske `DISTINCT`-tellinger. | Sammenlign alternativene, ikke opprett begge ukritisk. En partial-indeks med begge utfall krever fortsatt filtrering for telling av bare `FATT_JOBBEN` dersom utfall ikke er tilgjengelig som indekskolonne. |
| 3 | Dekkende indeks for kandidatlistens brede aggregater, med `stilling_opprettet_tidspunkt`, `er_direktemeldt` og relevante tellekolonner | Kan gi mindre lesing gjennom index-only scan. | Ikke anbefalt som første tiltak. Boolean- og datofiltre kan treffe det meste av tabellen. Test smale alternativer fremfor én stor indeks som forsøker å dekke alt. |

Ikke filtrer bort andre utfall før `max(id)` beregnes. Da kan et tidligere `FATT_JOBBEN` feilaktig telles etter at siste hendelse har reversert utfallet. Ikke bytt mellom høyeste `id` og høyeste hendelsestidspunkt.

## Avklaringer før endelig plan

1. Kommer observasjonen om mange `seq scan` fra produksjon, og finnes aggregerte tall fra `pg_stat_user_tables`, faktiske indeksdefinisjoner og eventuelle `EXPLAIN`-planer? Vi trenger radantall, tabell-/indeksstørrelse og måleperioden siden statistikken ble nullstilt. Ikke del personrader eller parametere med personopplysninger.
2. Avklart: Denne omgangen begrenses til databasetiltak uten SQL-omskriving. Vurder indekser og planleggerstatistikk, og behold omskriving som senere oppgave.
3. Skal produksjonsmålinger utføres av utvikleren og deles som anonymiserte planer, eller finnes et godkjent testmiljø med representativ datamengde? Planleggingen forutsetter ikke tilgang til produksjon fra denne sesjonen.

## Arbeidsoppgaver

### 1. Etablere målegrunnlag

Kartlegg faktisk PostgreSQL-versjon, `pg_indexes` og `pg_index.indisvalid` for de fire tabellene. Samle radestimater, størrelse, `seq_scan`, `seq_tup_read`, `idx_scan`, tidspunkt for statistikkreset og vedlikeholdsstatus. Høyt antall sekvensielle skann alene dokumenterer ikke et ytelsesproblem.

Bruk `pg_stat_statements` hvis det allerede er aktivert til å rangere de konkrete metrikkspørringene etter samlet kjøretid, antall kall, blokker lest og midlertidig I/O. Ikke installer utvidelser eller endre databaseinnstillinger som del av denne oppgaven.

Ta baseline for alle 21 metrikkspørringene. Knytt hver plan til repository-metoden og noter hvor stor del av tabellene den faktisk leser. Dersom produksjonsmålinger ikke er tilgjengelige, merk analysen som foreløpig.

`EXPLAIN ANALYZE` kjører spørringen og kan belaste databasen. Begynn med `EXPLAIN` uten kjøring i produksjon. Kjør `EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)` i godkjent miljø med avtalt `statement_timeout`, én spørring om gangen. Bruk read-only transaksjon der det passer. Ikke kjør `ANALYZE` eller endre statistikk i produksjon uten avklaring.

#### Første målerunde i produksjon

Start med metadata nedenfor og tre representative metrikkspørringer. Disse dekker siste kandidatutfall, siste kandidatlistehendelse og kontaktinfojoiner. De erstatter ikke baseline for alle 21 metoder før endelig indeksvalg.

Metadataene leser ikke personrader:

```sql
SELECT
    relname AS tabell,
    n_live_tup AS estimerte_rader,
    pg_size_pretty(pg_table_size(relid)) AS tabellstorrelse,
    pg_size_pretty(pg_indexes_size(relid)) AS indeksstorrelse,
    seq_scan,
    seq_tup_read,
    idx_scan,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
  AND relname IN (
      'kandidatliste',
      'kandidatutfall',
      'visning_kontaktinfo',
      'stilling'
  )
ORDER BY pg_table_size(relid) DESC;

SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
      'kandidatliste',
      'kandidatutfall',
      'visning_kontaktinfo',
      'stilling'
  )
ORDER BY tablename, indexname;

SELECT
    version(),
    stats_reset
FROM pg_stat_database
WHERE datname = current_database();
```

Skann-tellerne er kumulative og gjelder alle spørringer mot tabellen. `stats_reset` gir kontekst, men enkelte tabelltellere kan ha blitt nullstilt separat.

**Kjør én plan om gangen utenom belastningstopper.** `EXPLAIN ANALYZE` kjører hele spørringen, selv i en read-only transaksjon. Eksemplene setter 10 sekunders timeout og ett sekunds låsetimeout. Begynn med å erstatte `EXPLAIN (ANALYZE, BUFFERS, SETTINGS)` med `EXPLAIN (COSTS, SETTINGS)` for å få estimater uten å kjøre selve spørringen.

Ved timeout: Kjør `ROLLBACK;` før neste spørring. Ikke øk timeout automatisk. Hent heller den estimerte planen uten `ANALYZE`.

##### Siste kandidatutfall og join mot kandidatliste

Fra `hentAntallDirektemeldteStillingerMedMinstEnPresentertKandidat()`. Grupperingen med `max(id)` finnes i fem metrikkspørringer.

```sql
BEGIN READ ONLY;
SET LOCAL statement_timeout = '10s';
SET LOCAL lock_timeout = '1s';

EXPLAIN (ANALYZE, BUFFERS, SETTINGS)
WITH id_siste_utfall_per_kandidat_per_liste AS (
    SELECT max(id)
    FROM kandidatutfall
    GROUP BY aktorid, kandidatlisteid
),
presentert_utfall AS (
    SELECT *
    FROM kandidatutfall
    WHERE id IN (
        SELECT * FROM id_siste_utfall_per_kandidat_per_liste
    )
      AND (utfall = 'FATT_JOBBEN' OR utfall = 'PRESENTERT')
)
SELECT count(DISTINCT kandidatliste.kandidatliste_id)
FROM kandidatliste
INNER JOIN presentert_utfall
    ON presentert_utfall.kandidatlisteid = kandidatliste.kandidatliste_id
INNER JOIN stilling
    ON kandidatliste.stillings_id = stilling.uuid
WHERE kandidatliste.er_direktemeldt IS TRUE
  AND kandidatliste.stilling_opprettet_tidspunkt IS NOT NULL
  AND (
      stilling.stillingskategori = 'STILLING'
      OR stilling.stillingskategori IS NULL
  );

ROLLBACK;
```

Se hvor mye arbeid som går til global gruppering og til joinene. Kandidaten `(aktorid, kandidatlisteid, id DESC)` kan gi en smalere lesesti, men global gruppering kan fortsatt lese hele indeksen.

##### Siste hendelsestidspunkt per kandidatliste

Fra `hentAntallStillingerForStillingsannonserMedKandidatliste()`. To andre metoder bruker samme mønster med ekstra filter på `er_direktemeldt`.

```sql
BEGIN READ ONLY;
SET LOCAL statement_timeout = '10s';
SET LOCAL lock_timeout = '1s';

EXPLAIN (ANALYZE, BUFFERS, SETTINGS)
WITH id_og_siste_tidspunkt_for_direktemeldte_stillinger AS (
    SELECT
        first_value(tidspunkt_for_hendelsen) OVER (
            PARTITION BY kandidatliste_id
            ORDER BY tidspunkt_for_hendelsen DESC
        ) AS tidspunkt,
        kandidatliste_id
    FROM kandidatliste
    WHERE stilling_opprettet_tidspunkt IS NOT NULL
    GROUP BY kandidatliste_id, tidspunkt_for_hendelsen
)
SELECT DISTINCT
    kandidatliste.kandidatliste_id,
    kandidatliste.antall_stillinger
FROM kandidatliste
JOIN id_og_siste_tidspunkt_for_direktemeldte_stillinger AS siste
    ON siste.kandidatliste_id = kandidatliste.kandidatliste_id
   AND siste.tidspunkt = kandidatliste.tidspunkt_for_hendelsen;

ROLLBACK;
```

Se etter kostbar sortering, `WindowAgg` og join tilbake til tabellen. Prøv senere `(kandidatliste_id, tidspunkt_for_hendelsen DESC)`, eventuelt med `INCLUDE (antall_stillinger)`, mot denne baselinen.

##### Kontaktinfovisninger med historikkjoiner

Fra `hentAntallKandidatlisterMedMinstEnKandidatSomHarFåttVistSinKontaktinfoPerMåned()`.

```sql
BEGIN READ ONLY;
SET LOCAL statement_timeout = '10s';
SET LOCAL lock_timeout = '1s';

EXPLAIN (ANALYZE, BUFFERS, SETTINGS)
SELECT
    concat(
        extract(year FROM visning_kontaktinfo.tidspunkt)::text,
        '-',
        extract(month FROM visning_kontaktinfo.tidspunkt)::text
    ) AS maaned,
    count(DISTINCT stilling_id)
FROM visning_kontaktinfo
INNER JOIN kandidatliste
    ON kandidatliste.stillings_id = visning_kontaktinfo.stilling_id::text
INNER JOIN kandidatutfall
    ON visning_kontaktinfo.stilling_id::text = kandidatutfall.stillingsid
   AND visning_kontaktinfo.aktør_id = kandidatutfall.aktorid
WHERE kandidatliste.stilling_opprettet_tidspunkt >= '2023-03-01'
GROUP BY maaned;

ROLLBACK;
```

Undersøk join-indeksene på `kandidatliste (stillings_id)` og `kandidatutfall (stillingsid, aktorid)`. Se også om mange historikkrader gir et stort mellomresultat før `count(DISTINCT ...)`. I så fall kan en separat SQL-omskriving være nødvendig, men den inngår ikke uten avklaring.

Ta vare på hele planutskriften og metadataresultatene, ikke bare `Seq Scan`-linjene. Noter faktisk kjøretid, `actual rows` ganger `loops`, bufferlesing, disksortering og avvik mellom estimerte og faktiske radantall. Spørringene over returnerer ingen personrader.

### 2. Prøve og velge et lite indekssett

Prøv prioritet 1-kandidatene enkeltvis mot samme datasett og samme SQL som baseline. Oppdater statistikk i testmiljøet ved behov. Mål også kumulativ effekt og forkast overlappende indekser uten tilleggseffekt.

Registrer kjøretid, shared buffers hit/read, temp read/write, sortering, hash-batcher og heap fetches ved index-only scan. Sammenlign flere kjøringer med samme cacheforutsetninger. Ikke bruk `enable_seqscan = off` som bevis på gevinst.

Mål innsettingskostnad og indeksstørrelse for valgt sett. Testdata må ha representativ historikk per liste/kandidat, fordeling av utfall, nullverdier og kontaktinfovisninger. Små funksjonstester er ikke ytelsesbevis.

Aksept: Uendrede resultater for alle 21 metoder på uendret datasett. De valgte indeksene skal redusere median kjøretid og buffer-/I/O-arbeid for de prioriterte flaskehalsene sammenlignet med baseline, uten en regresjon større enn målevariasjonen i resten av jobben. Tallfest forventet totalgevinst og akseptabel skrivekostnad etter baseline, før migrasjonene ferdigstilles.

### 3. Lage sikre Flyway-migrasjoner

Begynn på neste ledige versjon, nå V19. Ikke endre V1–V18. Ingen unike indekser eller nye constraints inngår.

For store, aktive tabeller: én `CREATE INDEX CONCURRENTLY` per migrasjonsfil. Bruk tilhørende Flyway-konfigurasjon med `executeInTransaction=false`, og verifiser om `postgresql.transactional.lock=false` kreves for concurrent-indekser med repoets Flyway-versjon. Verifiser hele forløpet i integrasjonstest før deploy. Unngå globale transaksjonsendringer hvis migrasjonsspesifikk konfigurasjon er tilstrekkelig.

Concurrent indeksbygging kan fortsatt vente på transaksjoner og bruke mye I/O. Avtal byggerekkefølge og kjør én indeks om gangen på samme tabell. Flyway kjører ved appoppstart, så lange migrasjoner må vurderes mot deploymentens oppstartsgrenser før strategi velges.

Ikke bruk `IF NOT EXISTS` som automatisk gjenoppretting. En avbrutt concurrent-bygging kan etterlate en ugyldig indeks med samme navn. Kontroller definisjon og gyldighet, og beskriv eksplisitt opprydding og eventuell Flyway-reparasjon etter godkjent prosedyre.

### 4. Verifisere migrasjoner og uendret oppførsel

Kjør først eksisterende relevante databaseintegrasjonstester:

```bash
./gradlew test \
  --tests 'no.nav.statistikkapi.kandidatliste.KandidatlisteRepositoryTest' \
  --tests 'no.nav.statistikkapi.db.KandidatutfallRepositoryTest' \
  --tests 'no.nav.statistikkapi.visningkontaktinfo.VisningKontaktinfoRepositoryTest'
```

Docker er nødvendig. Verifiser migrasjon fra V18 med data og fra tom database, deretter ny oppstart uten nye migrasjoner. Kontroller at indeksene har forventet definisjon og er gyldige. Kjør `./gradlew check` før leveranse.

Ved ren indeksendring skal ingen produksjonsspørringer omskrives. Hvis SQL-omskriving senere godkjennes, må manglende karakteriseringstester legges til før endringen. Dekk reverserte utfall, flere hendelser med likt tidspunkt, hendelser ute av rekkefølge, nullverdier og månedstelling. Eksisterende testnavn viser dekning for flere slike tilfeller, men alle testkroppene er ikke gjennomgått i denne planfasen.

### 5. Dokumentere deploy og kontrollere effekt

Oppdater eksisterende metrikkavsnitt i `README.md` eller relevant eksisterende driftsdokumentasjon med valgt indekssett, bygging og rollback. Ikke opprett et separat sammendragsdokument.

Sammenlign oppstartsloggens varighet og metrikkjobbens samlede varighet før/etter. Følg database-CPU, I/O, låseventing og hendelses-/Kafka-etterslep under indeksbygging og etterpå. Bruk spørringsstatistikk til å kontrollere at gevinsten kommer fra de identifiserte spørringene. Lavere `seq_scan` er ikke et selvstendig akseptkriterium.

En app-rollback fjerner ikke indeksene, men gamle applikasjonsversjoner skal kunne bruke det additive skjemaet. Ved behov fjernes bare de nye indeksene gjennom en godkjent, separat prosedyre med `DROP INDEX CONCURRENTLY`. Ikke rediger anvendte migrasjoner eller slett Flyway-historikk.

## Rød sone og review

🔴 Rød sone: Ingen endring av forretningslogikk i den foreslåtte første leveransen. Valg av produksjonsmigrasjonsstrategi og tolkning av spørringsplaner krever utviklerens gjennomgang. SQL-omskriving som berører tellelogikk inngår ikke uten ny avklaring.

| Perspektiv | Foreløpig vurdering |
|------------|---------------------|
| Sikkerhet | Ingen ny tilgang eller persondataflyt. Del bare aggregerte målinger og planer uten personverdier. Produksjonsspørringer krever godkjent tilgang. |
| Plattform | Concurrent-bygging og Flyway under oppstart må prøves og avstemmes mot oppstartsgrenser. Poolstørrelse og Nais-oppsett beholdes. |
| Arkitektur | Additive indekser er første tiltak. Ingen ny lagringsmodell eller bakgrunnsjobb. |
| Endringssikkerhet | Eksisterende SQL beholdes. Migrasjonstester, likhetskontroll av resultater og før-/ettermålinger kreves. |
| Brukeropplevelse | Ingen UI-endring. Statistikkverdier og oppdateringsfrekvens skal være uendret. |

## Avgrenset PostgreSQL-vurdering

| Akse | Undersøkt og begrensning |
|------|------------------------|
| Indekser | Gjennomgått alle metrikkmetodene og søkt etter indekser/constraints i V1–V18. Mangler sekundærindekser i versjonert skjema. Produksjonsskjema er ukjent. |
| EXPLAIN | Ingen planer tilgjengelige. Ingen indeksgevinst er målt eller bekreftet. |
| Kolonne- og radavgrensning | Brede historiske aggregater, `SELECT *` i mellomresultater og gjentatte siste-per-gruppe-beregninger er observert. `LIMIT` på aggregatgrunnlaget ville endre resultatene og anbefales ikke. |
| JSONB | Ingen JSONB-spørringer i de undersøkte metrikkmetodene. GIN er ikke relevant her. |
| Tilkoblingspool | Undersøkt `Database.kt`: maksimalt to forbindelser. Det beviser ikke poolproblemer, og poolendring inngår ikke. |
| Migrasjoner | Flyway kjøres ved oppstart. Eksisterende kode har ingen særskilt konfigurasjon for concurrent-indekser. |

Inspected: Hele MetrikkJobb.kt, KandidatlisteRepository.kt og VisningKontaktinfoRepository.kt; KandidatutfallRepository.kt linje 1–300 (inkludert begge globale metrikkmetoder); Database.kt; TestDatabase.kt; application.kt linje 170–210; tabellopprettelsene V1, V10, V12 og V14; indeks-/constraint-søk i V1–V18; relevante testnavn; README.md og .github/copilot-instructions.md.

Mottatt produksjonsevidens: Brukerens uttrekk av tabellstatistikk og indeksdefinisjoner for de fire tabellene er vurdert. Uttrekket bekrefter at bare primærnøkkelindeksene finnes.

Mottatt miljøinformasjon: Produksjon rapporterer PostgreSQL 18.6 og statistikkreset `2026-10-27T07:58:40.158201Z`. Alle tre representative spørringsplaner er vurdert. Den første viser 10,68 sekunders kjøretid, mange primærnøkkeloppslag og et stort mellomresultat. Kontaktinfoplanen viser 19,46 sekunder, 4,29 millioner mellomrader og disksortering. Siste-hendelsestidspunkt-planen viser 6,08 sekunder og omfattende sortering før 705 471 mellomrader reduseres til 42 237 resultatpar.

Not inspected: Ingen direkte tilgang til produksjonsdatabasen er brukt. Full radfordeling, indeksgyldighet og de øvrige spørringsplanene er ikke tilgjengelige. Testkroppene og alle øvrige repository-metoder er ikke fullstendig gjennomgått.

Findings: Ingen dokumenterte blokkerende funn. To forhold må avklares før implementering: indeksbehovet mangler runtime-bevis, og concurrent-migrasjoner trenger verifisert Flyway-/oppstartsstrategi.

Verdict: CONCERNS. Kildekoden gir konkrete indekshypoteser, men ikke grunnlag for å love ytelsesgevinst eller beslutte endelig indekssett.
