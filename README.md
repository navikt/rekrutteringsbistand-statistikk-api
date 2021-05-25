# rekrutteringsbistand-statistikk-api
Aggregering, lagring og behandling av formidlingsstatistikk og annen statistikk knyttet til inkludering.

Starte appen:
Høyreklikk på `LokalApplication.kt` i IntelliJ og velg `Run`.


Får statistikk fra to kilder:
- Presentering av kandidater (`presenterKandidater` i `VeiledersKandidatlisteController`)
- Endring av utfall fra dropdown (`settUtfall` i `VeiledersKandidatlisteController`)

`rekrutteringsbistand-kandidat` (frontend) kaller `pam-kandidatsok-api` (backend) som igjen kaller `rekrutteringsbistand-statistikk-api` (backend) og lagrer i databasen.

For hver endring av utfall på en kandidat, lagres en ny rad i databasen.

Kan sjekke formidlingene tre plasser:
- Amplitude: https://analytics.amplitude.com/nav/dashboard/rp6el7n
- Grafana: https://grafana.adeo.no/d/GhdRa3mMz/rekrutteringsbistand-statistikk-api
- Sjekke i databasen: 
    - Gå i Vault: https://vault.adeo.no/
    - Åpne console i Vault og kjør: `vault read postgresql/prod-fss/creds/rekrutteringsbistand-statistikk-admin`
    - Bruk favoritt SQL-reader i tynnklient
    - JDBC-URL: `jdbc:postgresql://A01DBVL011.adeo.no:5432/rekrutteringsbistand-statistikk`
    - Logg inn med brukernavn og passord fra Vault
    
Diverse dokumentasjon ligger i [doc mappa](./doc).

## Koble til H2-database med IntelliJ
- Bytt ut `jdbcUrl` i `TestDatabase.kt` til `jdbc:h2:~/test;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE`. Da lagres databasen til en fil `test` på home path.
- I IntelliJ, velg "Database", new DataSource, H2 og lim inn samme URL og koble til.
- Høyreklikk på databasen og velg "Open query console" for å kunne gjøre spørringer mot databasen.

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

# Henvendelser

## For Nav-ansatte

* Dette Git-repositoriet eies av [Team inkludering i Produktområde arbeidsgiver](https://navno.sharepoint.com/sites/intranett-prosjekter-og-utvikling/SitePages/Produktomr%C3%A5de-arbeidsgiver.aspx).
* Slack-kanaler:
  * [#inkludering-utvikling](https://nav-it.slack.com/archives/CQZU35J6A)
  * [#arbeidsgiver-utvikling](https://nav-it.slack.com/archives/CD4MES6BB)
  * [#arbeidsgiver-general](https://nav-it.slack.com/archives/CCM649PDH)

## For folk utenfor Nav

* Opprett gjerne en issue i Github for alle typer spørsmål
* IT-utviklerne i Github-teamet https://github.com/orgs/navikt/teams/arbeidsgiver
* IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)
