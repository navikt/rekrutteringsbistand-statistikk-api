FROM ghcr.io/navikt/baseimages/temurin:17
COPY ./nais/init.sh /init-scripts/init.sh
COPY ./build/libs/rekrutteringsbistand-statistikk-api-all.jar app.jar

EXPOSE 8111
