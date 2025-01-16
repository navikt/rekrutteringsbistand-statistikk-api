FROM gcr.io/distroless/java21-debian12:nonroot
ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /

USER nonroot
ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
