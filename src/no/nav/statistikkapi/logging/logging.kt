package no.nav.statistikkapi.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

/**
 * Styrer logging til [secureLog](https://doc.nais.io/observability/logs/#secure-logs), forutsatt riktig konfigurasjon av Logback.
 *
 * Brukes ved å dekorere en hvilken som helst, vanlig org.slf4j.Logger slik:
 * ```
 * import no.nav.statistikkapi.logging.SecureLogLogger.Companion.secure
 * ...
 * secure(log).info(msg)
 * ```
 *
 * For at dette skal virke må appens fil `logback.xml` bruke appendere med filtere slik at logging events som har en marker med navn `SECURE_LOG` styres til riktig loggfil:
 * ```
 * <configuration>
 *     <appender name="appLog" ...>
 *         ...
 *         <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
 *             <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
 *                 <marker>SECURE_LOG</marker>
 *             </evaluator>
 *             <OnMismatch>NEUTRAL</OnMismatch>
 *             <OnMatch>DENY</OnMatch>
 *         </filter>
 *     </appender>
 *
 *     <appender name="secureLog" ...>
 *         ...
 *         <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
 *             <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
 *                 <marker>SECURE_LOG</marker>
 *             </evaluator>
 *             <OnMismatch>DENY</OnMismatch>
 *             <OnMatch>NEUTRAL</OnMatch>
 *         </filter>
 *     </appender>
 *
 *     <root ...>
 *         <appender-ref ref="appLog"/>
 *         <appender-ref ref="secureLog"/>
 *     </root>
 * </configuration>
 * ```
 * Se [offisiell Logback-dokumentasjon](https://logback.qos.ch/manual/filters.html#evaluatorFilter)
 *
 */
class SecureLogLogger private constructor(private val l: Logger) {

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


    companion object {
        fun secure(l: Logger): SecureLogLogger = SecureLogLogger(l)
    }

}
