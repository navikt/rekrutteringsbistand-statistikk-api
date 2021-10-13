val kotlinVersion =
// Det er minst sannsynlighet for dependency-plunder når vi bruker samme versjon av Kotlin som den som er bundlet med Gradle via gradlew.
// For å se hvilken versjon det er, kjør "./gradlew --version".
// Kotlin-versjonen oppgraderes slik: https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper
    embeddedKotlinVersion

val kotlinCodeStyle = "official"
val logbackVersion = "1.2.6"
val ktorVersion = "1.6.4"
val h2Version = "1.4.200"
val flywayVersion = "8.0.0"
val hikariVersion = "5.0.0"
val logstashEncoderVersion = "6.6"
val vaultJdbcVersion = "1.3.7"
val postgresVersion = "42.2.24"
val tokenValidationVersion = "1.3.8"
val jacksonVersion = "2.13.0"
val assertkVersion = "0.25"
val micrometerPrometheusVersion = "1.7.4"
val kafkaClientsVersion = "2.8.0"
val mockkVersion = "1.12.0"
val kafkaEmbeddedEnvironmentVersion = "2.8.0"
val kafkaAvroSerializerVersion = "6.2.1"
val shedlockVersion = "4.28.0"
val pitestVersion = "1.7.0"
val elasticSearchClientVersion = "7.10.1"
val kotlinLoggingVersion = "2.0.11"
val jsonassertVersion = "1.5.0"
val plotlyktServerVersion = "0.3.0"


plugins {
    application
    kotlin("jvm") version embeddedKotlinVersion

    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.1"
    id("info.solidsoft.pitest") version "1.7.0"

    idea
}

apply(plugin = "info.solidsoft.pitest")
apply(plugin = "kotlin")
apply(plugin = "application")
apply(plugin = "com.github.johnrengelman.shadow")

pitest {
    targetClasses.set(setOf("statistikkapi.*"))
    targetTests.set(setOf("statistikkapi.*"))
    useClasspathFile.set(true)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "16"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "16"
    }
}



application {
    mainClass.set("statistikkapi.ApplicationKt")
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
    maven {
        url = uri("https://dl.bintray.com/mipt-npm/dev")
    }
    maven("https://dl.bintray.com/mipt-npm/dataforge")
    maven("https://dl.bintray.com/mipt-npm/kscience")
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
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

    implementation("kscience.plotlykt:plotlykt-server:$plotlyktServerVersion")

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
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:$pitestVersion")
    testImplementation(kotlin("test"))
}

configurations.all {
    resolutionStrategy {
        force("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    }
}
