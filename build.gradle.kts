val logbackVersion = "1.5.25"
val ktorVersion = "3.4.3"
val flywayVersion = "9.7.0"
val hikariVersion = "5.0.1"
val logstashEncoderVersion = "9.0"
val vaultJdbcVersion = "1.3.10"
val postgresVersion = "42.7.10"
val tokenValidationVersion = "5.0.14"
val jacksonVersion = "2.14.0"
val assertkVersion = "0.25"
val micrometerPrometheusVersion = "1.10.0"
val kafkaClientsVersion = "4.2.0"
val mockkVersion = "1.13.2"
val kafkaAvroSerializerVersion = "7.8.0"
val shedlockVersion = "4.42.0"
val pitestVersion = "1.9.0"
val kotlinLoggingVersion = "2.0.11"
val jsonassertVersion = "1.5.1"
val mockOAuth2ServerVersion = "3.0.3"
val avroVersion = "1.12.0"
val testcontainersVersion = "1.21.4"
val jvmVersion = 25


plugins {
    application
    kotlin("jvm") version "2.3.21"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("info.solidsoft.pitest") version "1.19.0"
    idea
}

pitest {
    targetClasses = setOf("no.nav.statistikkapi.*")
    targetTests = setOf("no.nav.statistikkapi.*")
    useClasspathFile = true
}

kotlin {
    jvmToolchain(jvmVersion)
}
java { // Nødvendig fordi Avro genererer Java kildekode
    toolchain.languageVersion.set(JavaLanguageVersion.of(jvmVersion))
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
    // BOMs
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.github.navikt:rapids-and-rivers:2026021921161771532161.7a37f8c9e0cc")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion") {
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }
    implementation("io.ktor:ktor-client-apache")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-jackson")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-metrics-micrometer")
    implementation("io.ktor:ktor-server-netty")
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("no.nav.security:token-validation-ktor-v3:$tokenValidationVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    runtimeOnly("io.ktor:ktor-client-auth")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2025.01.10-08.49-9e6f64ad")
    testImplementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:$pitestVersion")
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation("io.ktor:ktor-server-cio")
    testImplementation("io.ktor:ktor-server-test-host") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation(kotlin("test"))
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
}
