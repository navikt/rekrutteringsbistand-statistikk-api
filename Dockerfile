FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /
ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
