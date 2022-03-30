NAV har et eget web-grensesnitt for å lese meldinger på en Kafka-topic:
- dev: https://nada-kafka-reader.nais.preprod.local/
- prod: https://nada-kafka-reader.nais.adeo.no/

Brukernavn og passord er servicebruker til applikasjonen. Disse ligger i Vault. Det er kun applikasjonen som har tilgang til å hente disse ut fra Vault.
Derfor må vi logge inn i podden manuelt for å hente ut brukernavn og passord:
`kubectl exec -it <pod> -- /bin/sh`
Brukernavn og passord ligger under `/secret/serviceuser`.

Alternativt ser det ut som det finnes en plugin til IntelliJ: https://www.jetbrains.com/help/idea/2021.3/big-data-tools-kafka.html#messaging  Ikke testet per 2022-03-30.
