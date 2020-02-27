FROM navikt/java:13
COPY ./build/libs/rekrutteringsbistand-statistikk-api-all.jar app.jar
ENV JAVA_OPTS='-Dlogback.configurationFile=logback-fss.xml'
