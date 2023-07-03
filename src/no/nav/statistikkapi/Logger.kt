package no.nav.statistikkapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val secureLog = LoggerFactory.getLogger("secureLog")


interface LoggerWithMarker {
    fun info(msg: String)

    fun info(msg: String, t: Throwable)
}

interface SecureLogLogger : LoggerWithMarker
interface AppLogLogger : LoggerWithMarker


private class LoggerWithMarkerImpl(private val m: Marker, private val l: Logger) : AppLogLogger, SecureLogLogger {
    override fun info(msg: String) {
        l.info(m, msg)
    }

    override fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

}

val Any.aresAppLog: AppLogLogger
    get() = LoggerWithMarkerImpl(MarkerFactory.getMarker("appLog"), LoggerFactory.getLogger(this::class.java))

val Any.aresSecureLog: SecureLogLogger
    get() = LoggerWithMarkerImpl(MarkerFactory.getMarker("secureLog"), LoggerFactory.getLogger(this::class.java))
