package no.nav.statistikkapi.logging

import org.slf4j.Logger
import org.slf4j.MarkerFactory

@Suppress("unused")
class SecureLog(private val logger: Logger) {
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    fun info(msg: String) = logger.info(teamLogsMarker, msg)
    fun info(msg: String, t: Throwable) = logger.info(teamLogsMarker, msg, t)

    fun warn(msg: String) = logger.warn(teamLogsMarker, msg)
    fun warn(msg: String, t: Throwable) = logger.warn(teamLogsMarker, msg, t)

    fun error(msg: String) = logger.error(teamLogsMarker, msg)
    fun error(msg: String, t: Throwable) = logger.error(teamLogsMarker, msg, t)
}
