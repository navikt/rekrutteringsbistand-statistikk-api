FROM gcr.io/distroless/java21-debian12:nonroot

RUN mkdir -p /home/nonroot && chown 1001:0 /home/nonroot
USER 1001
WORKDIR /home/nonroot

ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /
ENTRYPOINT ["java", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
