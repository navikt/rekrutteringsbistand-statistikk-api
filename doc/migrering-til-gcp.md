# Migrering fra FSS til GCP

Plan for å flytte `rekrutteringsbistand-statistikk-api` og databasen fra dev-fss/prod-fss til dev-gcp/prod-gcp. Vi bruker pg_dump-metoden fra Nais: [Migrating databases to GCP](https://doc.nais.io/persistence/cloudsql/how-to/migrating-databases-to-gcp/).

Vi kjører hele løpet i dev først og måler hvor lenge appen er nede, før vi gjør det samme i prod.

## Forarbeid

- [x] Oppdater ROS og PVK. Nais krever det før vi flytter, og dataene gjelder kandidater.
- [x] Avtal nedetid med de som bruker statistikken, og avtal når vi ikke lenger kan gå tilbake (se [Rollback](#rollback)).
- [x] Endre default branch fra `master` til `main` i GitHub. Den felles workflowen deployer bare fra `main`.

## Kodeendringer

- [x] `Cluster.kt`: Appen kjenner `dev-gcp` og `prod-gcp` i stedet for FSS-clusterne.
- [x] `Database.kt`: Appen kobler til med `DB_JDBC_URL` fra Cloud SQL i stedet for `HikariCPVaultUtil`. Flyway kjører uten `SET ROLE`.
- [x] `build.gradle.kts`: Avhengigheten `no.nav:vault-jdbc` er fjernet.
- [x] Bryteren `KUN_FLYWAY_MIGRERING` (`kun_flyway_migrering` i `nais/<miljø>-gcp.json`) får appen til å kjøre Flyway og bare svare på `/isalive` og `/isready`. Appen leser ikke fra rapiden, starter ikke Datavarehus-scheduleren og skriver ikke til `shedlock`. Bryteren er på i begge miljøene.

## Nais-manifest og workflow

- [x] Manifestet ligger i `nais/nais.yaml`, med verdier per miljø i `nais/dev-gcp.json` og `nais/prod-gcp.json`.
- [x] `gcp.sqlInstances` bruker `POSTGRES_18` og databasen `rekrutteringsbistand-statistikk` med `envVarPrefix: DB`. Prod har `db-custom-1-3840` og point-in-time recovery.
- [x] `vault`, `webproxy` og FSS-ingressene er fjernet. Ingressene er nå `intern.dev.nav.no` og `intern.nav.no`. Outbound-regelen mot `logging` i `nais-system` står igjen uten `cluster`, fordi team-logs sender dit.
- [x] Inbound-reglene for `rekrutteringsbistand-frontend` (og `rekrutteringsbistand-frontend2` i dev) har ikke `cluster` lenger.
- [x] Behold `KAFKA_CONSUMER_GROUP_ID` (`...-rapidconsumer-5` i dev og `...-rapidconsumer-2` i prod). Da fortsetter appen fra offseten i FSS. Med en ny group og `KAFKA_RESET_POLICY=earliest` leser appen hele rapiden på nytt.
- [x] `deploy.yml` bruker `build-and-deploy.yaml@v17` fra `toi-github-actions-workflows`. Den deployer `nais/alerts.yaml` til prod-gcp, og Dockerfile tar base-imaget som build-argument.
- [x] Workflowene for DVH-topicet trigges av `main`.
- [x] `nais/init.sh` er fjernet.

Vault-secrets trenger vi ikke flytte. Appen monterer servicebrukeren `srv-rekbis-stat`, men koden bruker den ikke, og Dockerfile kjører ikke `nais/init.sh`. Cloud SQL erstatter databasebrukeren fra Vault.

Aiven-ACL-ene for `toi.rapid-1` og `toi.kandidatutfall` gjelder team og app, ikke cluster. Dem trenger vi ikke endre.

## Gjennomføring

Gjør stegene i denne rekkefølgen, først i dev og så i prod.

Ikke slå av bryteren før steg 9. Workflowen deployer til GCP ved hver merge til `main`, og uten bryteren begynner GCP-appen å lese fra rapiden med samme consumer group som FSS-appen.

1. [ ] Deploy appen til GCP med bryteren på. Nais oppretter Cloud SQL-instansen, og Flyway lager tabellene.
2. [ ] Gi oss selv tilgang med `nais cloudsql prepare --all-privileges` og `nais cloudsql grant`, og sjekk at tabellene finnes. Brukeren som importerer, trenger skrivetilgang til tabellene appbrukeren eier, og rollen `roles/cloudsql.admin`.
3. [ ] Sjekk at FSS og GCP står på samme Flyway-versjon (nå V18). Ikke merge nye migreringer under flyttingen.
4. [x] Deploy til FSS er fjernet fra `deploy.yml`.
5. [ ] Stopp appen i FSS. Slett Application-ressursen eller deploy med 0 replicas. HPA overstyrer `kubectl scale` i prod.
6. [ ] Ta dump fra en pod i FSS med imaget fra [navikt/gcp-migrering](https://github.com/navikt/gcp-migrering), og last den opp til en bucket. Se Nais-dokumentasjonen for kommandoene. Bruk en `pg_dump` med samme eller høyere versjon enn kilden (pg15), og disse flaggene:

   ```shell
   pg_dump -h <host> -d rekrutteringsbistand-statistikk-pg15 -U <bruker fra Vault> \
     --format=plain --no-owner --no-acl --data-only \
     --exclude-table=flyway_schema_history --exclude-table=shedlock \
     -Z 9 > dump.sql.gz
   ```

7. [ ] Importer dumpen med `gcloud sql import sql <instans> gs://<bucket>/dump.sql.gz --database=rekrutteringsbistand-statistikk --user=<egen GCP-bruker>`.
8. [ ] Kontroller dataene. Sammenlign antall rader per tabell, høyeste id, sekvensene og antall usendte rader til Datavarehus mellom FSS og GCP.
9. [ ] Slå av bryteren (sett `kun_flyway_migrering` til `"false"` i `nais/<miljø>-gcp.json`) og deploy på nytt. Appen leser nå fra rapiden og sender til Datavarehus.
10. [ ] Endre URL og scope i appene som bruker statistikk-api (se [Konsumenter](#konsumenter)).

## Konsumenter

Når appen flytter til et nytt cluster, får den en ny Azure-app. Konsumentene må derfor bytte scope, ikke bare URL:

- Scope: `api://<dev|prod>-gcp.toi.rekrutteringsbistand-statistikk-api/.default`
- URL i samme cluster: `http://rekrutteringsbistand-statistikk-api.toi/rekrutteringsbistand-statistikk-api/...`
- Outbound-regel mot `rekrutteringsbistand-statistikk-api` uten `cluster`

Kjente konsumenter:

- [ ] `rekrutteringsbistand-frontend` (dev og prod)
- [ ] `rekrutteringsbistand-frontend2` (dev)
- [ ] Sjekk `toi-helseapp` i `toi-rapids-and-rivers`, som refererer til appnavnet.

### Speiling til BigQuery

Naisjoben `rekrutteringsbistand-bigquery` i prod-fss leser tabellene `kandidatliste`, `kandidatutfall`, `stilling` og `visning_kontaktinfo` direkte fra FSS-databasen hver natt og speiler dem til BigQuery (`toi-prod-324e.rekrutteringsbistand_statistikk_pg15`). Jobben bruker Vault-brukere til databasen og kan ikke nå Cloud SQL.

- [ ] Finn ny løsning for speilingen av statistikkdatabasen, og fjern statistikk-delen fra jobben før FSS-databasen slettes.

## Rollback

FSS-databasen er urørt helt til GCP-appen begynner å lese fra rapiden (steg 9). Før det kan vi starte FSS-appen igjen uten å miste data. Etter steg 9 skriver GCP-appen data som FSS-databasen ikke har, og går vi tilbake, mister vi de dataene.

## Opprydding

- [ ] Slett bucketen med dumpen.
- [ ] Slett FSS-databasen og oppsettet for appen i `vault-iac`.
- [ ] Slett servicebrukeren `srv-rekbis-stat`. Fjern først monteringen i `nais.yaml` i `rekrutteringsbistand-bigquery`, ellers feiler Vault-oppstarten til jobben. Fjern også fila i `gruppetilgang-servicebruker`.
- [ ] Slett FSS-appen og fss-pub-ingressene.
- [ ] Oppdater README: databasetilgang via Vault, Grafana-lenker og loggspørringen med `cluster: prod-fss`.
- [ ] Oppdater eller slett `doc/hvordan-lese-topic.md` og `doc/hvordan-opprette-topic.md`, som viser til servicebrukeren og on-prem Kafka.
