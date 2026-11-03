## Kort oppsummering av hva vi gjorde for å sette opp federated query for å flytte data fra Postgres til BigQuery

Vi fulgte nais-docen [her](https://docs.knada.io/dataprodukter/dele/dataoverf%C3%B8ring/#federated-query).

I punkt 1 opprettet vi en secret (rekrutteringsbistand-statistikk-api-bigquery) i Nais console.
Den brukes av postgres-brukeren som ble opprettet med kommandoen

`nais cloudsql users add rekrutteringsbistand-statistikk-api bigquery <passord> \
-e prod-gcp -t toi -r "Federated query til BigQuery"`.

I punkt 2 ble samme bruker/passord brukt for å sette opp en Cloud SQL Connection (rekrutteringsbistand-statistikk-api):
```
read -rs PW
bq mk --connection \
  --project_id=toi-prod-324e \
  --location=europe-north1 \
  --connection_type=CLOUD_SQL \
  --display_name="rekrutteringsbistand-statistikk-api" \
  --properties='{"instanceId":"toi-prod-324e:europe-north1:rekrutteringsbistand-statistikk-api","database":"rekrutteringsbistand-statistikk","type":"POSTGRES"}' \
  --connection_credential="{\"username\":\"bigquery\",\"password\":\"$PW\"}" \
  rekrutteringsbistand-statistikk-api
unset PW
Connection 565127870017.europe-north1.rekrutteringsbistand-statistikk-api successfully created
```

Deretter kjørte vi kommandoen under for å gi service-kontoen som BigQuery bruker tilgang til Cloud SQL Connection:
```
gcloud projects add-iam-policy-binding toi-prod-324e \
  --member="serviceAccount:service-565127870017@gcp-sa-bigqueryconnection.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

Opprettet nytt datasett i BigQuery for rekrutteringsbistand-statistikk-api:
```
bq mk --dataset \
  --description "Datasett for rekrutteringsbistand-statistikk-api" \
  --location=europe-north1 \
  toi-prod-324e:rekrutteringsbistand_statistikk_api
```

I punkt 5 gjenbrukte vi service bruker `bigquery-federated-queries` i toi-prod.

I punkt 6 gav vi service bruker `bigquery-federated-queries` tilgang til datasettet `rekrutteringsbistand_statistikk_api`:
```
bq --project_id=toi-prod-324e query --use_legacy_sql=false '
GRANT `roles/bigquery.dataEditor`
ON SCHEMA `toi-prod-324e.rekrutteringsbistand_statistikk_api`
TO "serviceAccount:bigquery-federated-queries@toi-prod-324e.iam.gserviceaccount.com";
'
```

Deretter opprettet vi en schedulert spørring basert på [speiling-db-rekrutteringsbistand-statistikk-api.sql](./federated-queries/speiling-db-rekrutteringsbistand-statistikk-api.sql) i BigQuery som kjører hver natt kl 04 UTC og speiler databasen fra Postgres til BigQuery:
```
bq mk --transfer_config \
  --project_id=toi-prod-324e \
  --location=europe-north1 \
  --data_source=scheduled_query \
  --display_name="speil-rekrutteringsbistand-statistikk-api" \
  --schedule="every day 04:00" \
  --service_account_name="bigquery-federated-queries@toi-prod-324e.iam.gserviceaccount.com" \
  --params="{\"query\":$(jq -Rs . < ./federated-queries/speiling-db-rekrutteringsbistand-statistikk-api.sql)}"
```

Ved behov for å oppdatere spørringen, rediger filen [speiling-db-rekrutteringsbistand-statistikk-api.sql](./federated-queries/speiling-db-rekrutteringsbistand-statistikk-api.sql) og oppdater federated query i BigQuery:
```
bq update --transfer_config \
  --params="{\"query\":$(jq -Rs . < ./federated-queries/speiling-db-rekrutteringsbistand-statistikk-api.sql)}" \
  projects/565127870017/locations/europe-north1/transferConfigs/6aeb2d7b-0000-231c-92ea-089e08267044
```