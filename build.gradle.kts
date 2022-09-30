/** Det er minst sannsynlighet for dependency-plunder når vi bruker samme versjon av Kotlin som den som er bundlet med Gradle via gradlew.
For å se hvilken versjon det er, kjør "./gradlew --version".
Kotlin-versjonen oppgraderes slik: https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper
 */
val kotlinCodeStyle = "official"
val logbackVersion = "1.4.1"
val ktorVersion = "2.1.1"
val ktorLesserVersion = "1.6.8"
val h2Version = "2.1.214"
val flywayVersion = "9.4.0"
val hikariVersion = "5.0.0"
val logstashEncoderVersion = "7.2"
val vaultJdbcVersion = "1.3.10"
val postgresVersion = "42.5.0"
val tokenValidationVersion = "2.1.5"
val jacksonVersion = "2.13.4"
val assertkVersion = "0.25"
val micrometerPrometheusVersion = "1.9.4"
val kafkaClientsVersion = "3.3.0"
val mockkVersion = "1.13.2"
val kafkaAvroSerializerVersion = "6.0.1"
val shedlockVersion = "4.42.0"
val pitestVersion = "1.9.0"
val elasticSearchClientVersion = "7.10.1"
val kotlinLoggingVersion = "2.0.11"
val jsonassertVersion = "1.5.1"
val mockOAuth2ServerVersion = "0.5.4"


plugins {
    application
    kotlin("jvm") version embeddedKotlinVersion // Kotlinversjon styres av gradlew, se https://blog.nishtahir.com/how-to-properly-update-the-gradle-wrapper/
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.1"
    id("info.solidsoft.pitest") version "1.7.0"
    idea
}

pitest {
    targetClasses.set(setOf("no.nav.statistikkapi.*"))
    targetTests.set(setOf("no.nav.statistikkapi.*"))
    useClasspathFile.set(true)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
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
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.navikt:rapids-and-rivers:2022061809451655538329.d6deccc62862")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("io.ktor:ktor-jackson:$ktorLesserVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("io.ktor:ktor-auth:$ktorLesserVersion")
    implementation("no.nav.security:token-validation-ktor-v2:$tokenValidationVersion") {
        exclude(group = "io.ktor", module = "ktor-auth")
    }
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerPrometheusVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("io.confluent:kafka-avro-serializer:$kafkaAvroSerializerVersion")
    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")

    testImplementation(kotlin("test"))
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:$pitestVersion")

    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
}

configurations.all {
    resolutionStrategy {
        force("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    }
}
