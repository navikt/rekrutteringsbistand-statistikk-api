package no.nav.statistikkapi.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

interface SecureLogLogger {
    val markerName: String

    fun info(msg: String)

    fun info(msg: String, t: Throwable)

    fun warn(msg: String)

    fun warn(msg: String, t: Throwable)

    fun error(msg: String)

    fun error(msg: String, t: Throwable)
}

fun secure(l: Logger): SecureLogLogger = object : SecureLogLogger {
    override val markerName: String = "SECURE_LOG"

    private val m: Marker = MarkerFactory.getMarker(markerName)

    override fun info(msg: String) {
        l.info(m, msg)
    }

    override fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

    override fun warn(msg: String) {
        l.warn(m, msg)
    }

    override fun warn(msg: String, t: Throwable) {
        l.warn(m, msg, t)
    }

    override fun error(msg: String) {
        l.error(m, msg)
    }

    override fun error(msg: String, t: Throwable) {
        l.error(m, msg, t)
    }

}



