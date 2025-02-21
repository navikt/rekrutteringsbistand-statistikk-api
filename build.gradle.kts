val logbackVersion = "1.4.4"
val ktorVersion = "3.0.1"
val flywayVersion = "9.7.0"
val hikariVersion = "5.0.1"
val logstashEncoderVersion = "7.2"
val vaultJdbcVersion = "1.3.10"
val postgresVersion = "42.7.4"
val tokenValidationVersion = "5.0.14"
val jacksonVersion = "2.14.0"
val assertkVersion = "0.25"
val micrometerPrometheusVersion = "1.10.0"
val kafkaClientsVersion = "3.9.0"
val mockkVersion = "1.13.2"
val kafkaAvroSerializerVersion = "7.8.0"
val shedlockVersion = "4.42.0"
val pitestVersion = "1.9.0"
val kotlinLoggingVersion = "2.0.11"
val jsonassertVersion = "1.5.1"
val mockOAuth2ServerVersion = "0.5.6"
val avroVersion = "1.12.0"


plugins {
    application
    kotlin("jvm") version "2.1.0"
    id("com.github.ben-manes.versions") version "0.43.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
    id("info.solidsoft.pitest") version "1.9.0"
    idea
}

pitest {
    targetClasses.set(setOf("no.nav.statistikkapi.*"))
    targetTests.set(setOf("no.nav.statistikkapi.*"))
    useClasspathFile.set(true)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    mainClass.set("no.nav.statistikkapi.ApplicationKt")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

repositories {
    mavenCentral()
    maven("https://jcenter.bintray.com/")
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")

    // Fiks poison pill
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.github.navikt:rapids-and-rivers:2025010715371736260653.d465d681c420")
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2025.01.10-08.49-9e6f64ad")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    runtimeOnly("io.ktor:ktor-client-auth:${ktorVersion}")
    implementation("io.ktor:ktor-server-auth-jvm:${ktorVersion}")

    implementation("no.nav.security:token-validation-ktor-v3:$tokenValidationVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")
    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:$pitestVersion")

    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")

    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:postgresql:1.17.5")
    testImplementation("io.ktor:ktor-server-cio:$ktorVersion")
}

