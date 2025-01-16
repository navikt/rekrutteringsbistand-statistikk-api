FROM gcr.io/distroless/java21-debian12:nonroot

ARG APP_NAME
WORKDIR /$APP_NAME

ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /
ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
