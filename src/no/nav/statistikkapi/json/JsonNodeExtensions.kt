package no.nav.statistikkapi.json

import tools.jackson.databind.JsonNode
import java.time.ZonedDateTime
import java.util.*

fun JsonNode.asZonedDateTime(): ZonedDateTime =
    asString().let(ZonedDateTime::parse)

fun JsonNode.asZonedDateTimeNullable(): ZonedDateTime? =
    asTextNullable()?.let(ZonedDateTime::parse)

fun JsonNode.asTextNullable() =
    if (isMissingNode || isNull) null else asString()

fun JsonNode.asIntNullable() = asTextNullable()?.toInt()

fun JsonNode.asBooleanNullable() = asTextNullable()?.toBoolean()

fun JsonNode.asUUID() = UUID.fromString(asString())
