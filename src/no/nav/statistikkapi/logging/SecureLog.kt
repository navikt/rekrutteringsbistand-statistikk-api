package no.nav.statistikkapi.logging

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.TODO

private val teamLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")

class SecureLog(private val logger: Logger): Logger {
    override fun getName() = logger.name
    override fun isTraceEnabled() = logger.isTraceEnabled
    override fun trace(msg: String?) = logger.trace(teamLogsMarker, msg)
    override fun trace(format: String?, arg: Any?) = logger.trace(teamLogsMarker, format, arg)
    override fun trace(format: String?, arg1: Any?, arg2: Any?) = logger.trace(teamLogsMarker, format, arg1, arg2)
    override fun trace(format: String?, vararg arguments: Any?) = logger.trace(teamLogsMarker, format, *arguments)
    override fun trace(msg: String?, t: Throwable?) = logger.trace(teamLogsMarker, msg, t)
    override fun isTraceEnabled(marker: Marker?): Boolean = logger.isTraceEnabled(marker)
    override fun isDebugEnabled() = logger.isDebugEnabled
    override fun debug(msg: String?) = logger.debug(teamLogsMarker, msg)
    override fun debug(format: String?, arg: Any?) = logger.debug(teamLogsMarker, format, arg)
    override fun debug(format: String?, arg1: Any?, arg2: Any?) = logger.debug(teamLogsMarker, format, arg1, arg2)
    override fun debug(format: String?, vararg arguments: Any?) = logger.debug(teamLogsMarker, format, *arguments)
    override fun debug(msg: String?, t: Throwable?) = logger.debug(teamLogsMarker, msg, t)
    override fun isDebugEnabled(marker: Marker?) = logger.isDebugEnabled(marker)
    override fun isInfoEnabled() = logger.isInfoEnabled
    override fun info(msg: String?) = logger.info(teamLogsMarker, msg)
    override fun info(format: String?, arg: Any?) = logger.info(teamLogsMarker, format, arg)
    override fun info(format: String?, arg1: Any?, arg2: Any?) = logger.info(teamLogsMarker, format, arg1, arg2)
    override fun info(format: String?, vararg arguments: Any?) = logger.info(teamLogsMarker, format, *arguments)
    override fun info(msg: String?, t: Throwable?) = logger.info(teamLogsMarker, msg, t)
    override fun isInfoEnabled(marker: Marker?) = logger.isInfoEnabled(marker)
    override fun isWarnEnabled() = logger.isWarnEnabled
    override fun warn(msg: String?) = logger.warn(teamLogsMarker, msg)
    override fun warn(format: String?, arg: Any?) = logger.warn(teamLogsMarker, format, arg)
    override fun warn(format: String?, vararg arguments: Any?) = logger.warn(teamLogsMarker, format, *arguments)
    override fun warn(format: String?, arg1: Any?, arg2: Any?) = logger.warn(teamLogsMarker, format, arg1, arg2)
    override fun warn(msg: String?, t: Throwable?) = logger.warn(teamLogsMarker, msg, t)
    override fun isWarnEnabled(marker: Marker?) = logger.isWarnEnabled(marker)
    override fun isErrorEnabled() = logger.isErrorEnabled
    override fun error(msg: String?) = logger.error(teamLogsMarker, msg)
    override fun error(format: String?, arg: Any?) = logger.error(teamLogsMarker, format, arg)
    override fun error(format: String?, arg1: Any?, arg2: Any?) = logger.error(teamLogsMarker, format, arg1, arg2)
    override fun error(format: String?, vararg arguments: Any?) = logger.error(teamLogsMarker, format, *arguments)
    override fun error(msg: String?, t: Throwable?) = logger.error(teamLogsMarker, msg, t)
    override fun isErrorEnabled(marker: Marker?) = logger.isErrorEnabled(marker)

    override fun trace(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }
}