FROM navikt/java:17
USER root
RUN apt-get update && apt-get install -y curl
USER apprunner
COPY ./nais/init.sh /init-scripts/init.sh
COPY ./build/libs/rekrutteringsbistand-statistikk-api-all.jar app.jar

EXPOSE 8111
