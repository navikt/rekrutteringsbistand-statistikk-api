FROM gcr.io/distroless/java21-debian12:nonroot

RUN mkdir -p /home/nonroot/

ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /
ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
