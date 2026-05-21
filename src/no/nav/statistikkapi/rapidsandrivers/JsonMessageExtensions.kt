package no.nav.statistikkapi.rapidsandrivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

fun JsonMessage.requireValueIfPresent(key: String, value: Boolean) {
    interestedIn(key)
    val node = this[key]
    if (!node.isMissingNode && !node.isNull) {
        requireValue(key, value)
    }
}
