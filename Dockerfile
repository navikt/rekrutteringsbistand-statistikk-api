FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /rekrutteringsbistand-statistikk-api
ADD build/distributions/rekrutteringsbistand-statistikk-api.tar /

# Important for Securelogs: The -cp command explicitly force using the logback.xml file of this project, to avoid accidental use of any logback.xml in it's dependencies, e.g. rapids-and-rivers.
# We did have an issue with logging meant for Securelogs accidentally ending up in ordinary applicaion logs.
ADD build/resources/main/logback.xml /
ENTRYPOINT ["java", "-Duser.timezone=Europe/Oslo", "-Dlogback.configurationFile=/logback.xml", "-cp", "/rekrutteringsbistand-statistikk-api/lib/*", "no.nav.statistikkapi.ApplicationKt"]
