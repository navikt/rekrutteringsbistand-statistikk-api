# rekrutteringsbistand-statistikk-api

Aggregering, lagring og behandling av formidlingsstatistikk og annen statistikk knyttet til inkludering.

Får statistikk fra to hendelser i Rekrutteringsbistand sitt GUI:

- Presentering av kandidater for arbeidsgiver
- Endring av utfall fra dropdown

`rekrutteringsbistand-kandidat` (frontend) kaller `rekrutteringsbistand-kandidat-api` (backend) som igjen kaller `rekrutteringsbistand-statistikk-api` (backend) og lagrer i databasen.

For hver endring av utfall på en kandidat, lagres en ny rad i databasen.

Kan sjekke formidlingene tre plasser:

- Amplitude: https://analytics.amplitude.com/nav/dashboard/rp6el7n
- Grafana: https://grafana.adeo.no/d/GhdRa3mMz/rekrutteringsbistand-statistikk-api
- Sjekke i databasen:
  - Gå i Vault: https://vault.adeo.no/
  - Åpne console i Vault og kjør: `vault read postgresql/prod-fss/creds/rekrutteringsbistand-statistikk-pg15-readonly`
  - Bruk favoritt SQL-reader i tynnklient
  - JDBC-URL: `jdbc:postgresql://prod-pg.intern.nav.no:5432/rekrutteringsbistand-statistikk`
  - Logg inn med brukernavn og passord fra Vault

Diverse dokumentasjon ligger i [doc mappa](./doc).


## Lokal bygging og kjøring

Kjøring av tester lokalt med f.eks. `./gradlew check` forutsetter at [Docker Engine](https://docs.docker.com/engine/) er installert på maskina di.

Starte appen lokalt:
Høyreklikk på `LokalApplication.kt` i IntelliJ og velg `Run`.

## Avro-skjema-kompabilitet

Schema-registryet på topicet er bruker ikke standard backward-compatibility, men bruker heller "forward transitive".
Dette kan man se ved å gjøre get-kall mot https://kafka-schema-registry.nais.preprod.local/config/aapen-formidlingsutfallEndret-v1-value
API-referanse: https://docs.confluent.io/platform/current/schema-registry/develop/api.html

## Kibana

`application: rekrutteringsbistand-statistikk-api AND cluster: prod-fss AND (level: Warning OR level: Error)`

## Alerts

`#inkludering-alerts-prod`

## Metrikker

https://grafana.adeo.no/d/GhdRa3mMz/rekrutteringsbistand-statistikk-api
https://analytics.amplitude.com/nav/dashboard/rp6el7n (nederst på siden)

## Opprettelse av datapakke i datakatalogen

Datapakke-id ble opprettet ved å kalle kjøre: curl -X 'POST' -d @datapakke.json 'https://datakatalog-api.dev.intern.nav.no/v1/datapackage'
med innehold i datapakke.json: {"title":"Hull i cv","description":"Vise hull i cv","views":[],"resources":[]}
(Id genereres basert på verdien av title)


# Testing av applikasjonen i Rekrutteringsbistand
- Del stilling med kandidat
- Gå inn i aktivitetsplanen og svar ja/nei på deling av CV
- Sjekk forsiden av Rekrutteringsbistand for å se om tellingen har blitt oppdatert som forventet
- Gå inn i databasen og sjekk at radene som er lagret blir markert med som sendt til datavarehus
- Hvis man gir jobben til kandidat, og så sjekke i databasen at PRESENTERT og FIKK_JOBBEN ble lagret og at de ble sendt til datavarehus


## Henvendelser

### For Nav-ansatte
* Dette Git-repositoriet eies av [team Toi](https://teamkatalog.nav.no/team/76f378c5-eb35-42db-9f4d-0e8197be0131).
* Slack: [#arbeidsgiver-toi-dev](https://nav-it.slack.com/archives/C02HTU8DBSR)

### For folk utenfor Nav
* IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)
