val kotlinCodeStyle = "official"
val logbackVersion = "1.2.3"
val ktorVersion = "1.4.1" // Håndterer unauthorized annerledes
val kotlinVersion = "1.4.30"
val h2Version = "1.4.200"
val flywayVersion = "7.5.3"
val hikariVersion = "4.0.2"
val logstashEncoderVersion = "6.5" // Denne er problematisk
val vaultJdbcVersion = "1.3.7"
val postgresVersion = "42.2.18"
val tokenValidationVersion = "1.3.3"
val jacksonVersion = "2.11.0" // Denne er problematisk
val assertkVersion = "0.23.1"
val micrometerPrometheusVersion = "1.6.3"
val kafkaClientsVersion = "2.7.0"
val mockkVersion = "1.10.6"
val kafkaEmbeddedEnvironmentVersion = "2.5.0"
val kafkaAvroSerializerVersion = "5.5.3" // Kan ikke oppgradere til 6.1.0 siden no.nav:kafka-embedded-env:$kafkaEmbeddedEnvironmentVersion baserer seg på 5.4
val shedlockVersion = "4.20.0"
val unleashClientJavaVersion = "4.0.1"

plugins {
    application
    kotlin("jvm") version "1.4.10"

    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("com.commercehub.gradle.plugin.avro") version "0.21.0"

    idea
}

apply(plugin = "kotlin")
apply(plugin = "application")
apply(plugin = "com.github.johnrengelman.shadow")

application {
    mainClassName = "no.nav.rekrutteringsbistand.statistikk.ApplicationKt"
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

repositories {
    jcenter()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")

    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenValidationVersion") {
        exclude(group = "io.ktor", module = "ktor-auth")
    }

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerPrometheusVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")
    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")
    implementation("no.finn.unleash:unleash-client-java:$unleashClientJavaVersion")

    testImplementation("no.nav.security:token-validation-test-support:$tokenValidationVersion") {
        exclude(group = "org.springframework.boot")
    }
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvironmentVersion")
}

