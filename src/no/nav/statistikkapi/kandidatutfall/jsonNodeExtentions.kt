package no.nav.statistikkapi.kandidatutfall

import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

fun JsonNode.asZonedDateTime() =
    asText().let ( ZonedDateTime::parse )

fun JsonNode.asTextNullable() = asText(null)

fun JsonNode.asIntNullable() = asTextNullable()?.toInt()