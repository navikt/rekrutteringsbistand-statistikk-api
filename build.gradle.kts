val kotlinCodeStyle = "official"
val logbackVersion = "1.2.1"
val ktorVersion = "1.3.0"
val kotlinVersion = "1.3.61"
val h2Version = "1.4.200"
val flywayVersion = "6.2.4"
val hikariVersion = "3.4.2"
val logstashEncoderVersion = "6.3"
val vaultJdbcVersion = "1.3.1"
val shadowVersion = "5.2.0"
val postgresVersion = "42.2.10"
val tokenValidationKtorVersion = "1.1.4"
val tokenValidationTestSupportVersion = "1.1.4"

plugins {
    application
    kotlin("jvm") version "1.3.61"

    id("com.github.johnrengelman.shadow") version "5.2.0"
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

    implementation("com.h2database:h2:$h2Version")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")

    implementation("no.nav.security:token-validation-ktor:$tokenValidationKtorVersion")
    implementation("no.nav.security:token-validation-test-support:$tokenValidationTestSupportVersion") {
        exclude(group = "org.springframework.boot")
    }


    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}
