FROM gcr.io/distroless/java21-debian12:debug-nonroot


RUN mkdir -p /test
WORKDIR test


ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /

ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
