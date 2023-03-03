package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.ZonedDateTime

fun JsonNode.asZonedDateTime(): ZonedDateTime =
    asText().let(ZonedDateTime::parse)

fun JsonNode.asZonedDateTimeNullable(): ZonedDateTime? =
    asTextNullable()?.let(ZonedDateTime::parse)

fun JsonNode.asTextNullable() = asText(null)

fun JsonNode.asIntNullable() = asTextNullable()?.toInt()

fun JsonNode.asBooleanNullable() = asTextNullable()?.toBoolean()

fun PrometheusMeterRegistry.incrementUtfallLagret(utfall: Utfall) =
    this
        .counter(
            "rekrutteringsbistand.statistikk.utfall.lagret",
            "utfall", utfall.name
        ).increment()


