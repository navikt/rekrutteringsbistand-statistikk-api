val logbackVersion = "1.5.32"
val ktorVersion = "3.4.3"
val flywayVersion = "12.6.1"
val hikariVersion = "7.0.2"
val logstashEncoderVersion = "9.0"
val vaultJdbcVersion = "1.3.10"
val postgresVersion = "42.7.11"
val tokenValidationVersion = "5.0.30"
val jacksonVersion = "2.21.3"
val assertkVersion = "0.28.1"
val kafkaClientsVersion = "4.2.0"
val mockkVersion = "1.14.9"
val kafkaAvroSerializerVersion = "8.2.1"
val shedlockVersion = "7.7.0"
val pitestVersion = "1.15.0"
val mockOAuth2ServerVersion = "4.0.0"
val avroVersion = "1.12.1"
val testcontainersVersion = "1.21.4"
val jvmVersion = 25


plugins {
    application
    kotlin("jvm") version "2.3.21"
    id("com.github.ben-manes.versions") version "0.54.0"
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
    implementation("com.github.navikt:rapids-and-rivers:2026071513121784113927")
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
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    runtimeOnly("io.ktor:ktor-client-auth")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:20260827.1253")
    testImplementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:$pitestVersion")
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation("io.ktor:ktor-client-apache5")
    testImplementation("io.ktor:ktor-server-cio")
    testImplementation("io.ktor:ktor-server-test-host") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation(kotlin("test"))
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
}
