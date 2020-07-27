NAV Kafka doc: https://confluence.adeo.no/display/AURA/Kafka

API for å opprette, slette og endre topics:
- dev: https://kafka-adminrest.nais.preprod.local/api/v1
- prod: https://kafka-adminrest.nais.adeo.no/api/v1

Logg inn med NAV-ident og passord via "Authorize"-knappen.
Bruk oneshot-endepunktet med følgende config for å opprette topic med evig retention. 
```
{
  "topics": [
    {
      "topicName": "privat-formidlingsutfallEndret-v1",
      "members": [
        { "member": "srv-rekbis-stat", "role": "PRODUCER" },
        { "member": "M154663", "role": "MANAGER" },
        { "member": "H152121", "role": "MANAGER" },
        { "member": "S154510", "role": "MANAGER" }
      ],
      "numPartitions": 1,
      "configEntries": {
        "retention.ms": "-1"
      }
    }
  ]
}
```

Doc for konfigurering av topic: https://kafka.apache.org/documentation/#topicconfigs
