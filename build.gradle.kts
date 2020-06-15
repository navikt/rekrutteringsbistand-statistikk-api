val kotlinCodeStyle = "official"
val logbackVersion = "1.2.1"
val ktorVersion = "1.3.2"
val kotlinVersion = "1.3.72"
val h2Version = "1.4.200"
val flywayVersion = "6.4.4"
val hikariVersion = "3.4.5"
val logstashEncoderVersion = "6.4"
val vaultJdbcVersion = "1.3.7"
val shadowVersion = "5.2.0"
val postgresVersion = "42.2.14"
val tokenValidationKtorVersion = "1.1.6"
val tokenValidationTestSupportVersion = "1.1.6"
val jacksonVersion = "2.11.0"
val assertkVersion = "0.22"
val micrometerPrometheusVersion = "1.5.1"

plugins {
    application
    kotlin("jvm") version "1.3.72"

    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.ben-manes.versions") version "0.28.0"
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
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.h2database:h2:$h2Version")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")

    implementation("no.nav.security:token-validation-ktor:$tokenValidationKtorVersion"){
        exclude(group = "io.ktor", module = "ktor-auth")
    }

    implementation("io.ktor:ktor-auth:$ktorVersion")

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerPrometheusVersion")

    implementation("no.nav.security:token-validation-test-support:$tokenValidationTestSupportVersion") {
        exclude(group = "org.springframework.boot")
    }

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
}
