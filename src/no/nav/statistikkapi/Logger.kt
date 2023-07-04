package no.nav.statistikkapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

/**
 * TODO Are: Beskriv her hva som er nødvendig å ha i fila logback.xml for at dette skal funke. https://trello.com/c/tve3ugkr
 */
class SecureLogLogger(private val l: Logger) {
    val markerName: String = "SECURE_LOG"

    private val m: Marker = MarkerFactory.getMarker(markerName)

    fun info(msg: String) {
        l.info(m, msg)
    }

    fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

    fun warn(msg: String) {
        l.warn(m, msg)
    }

    fun warn(msg: String, t: Throwable) {
        l.warn(m, msg, t)
    }

    fun error(msg: String) {
        l.error(m, msg)
    }

    fun error(msg: String, t: Throwable) {
        l.error(m, msg, t)
    }
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val Any.secureLog: SecureLogLogger
    get() = SecureLogLogger(LoggerFactory.getLogger(this::class.java))
