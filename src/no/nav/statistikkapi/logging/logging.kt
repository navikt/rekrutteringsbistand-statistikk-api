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

/**
 * Styrer logging til [secureLog](https://doc.nais.io/observability/logs/#secure-logs), forutsatt riktig konfigurasjon av Logback.
 *
 * Brukes ved å dekorere en hvilken som helst, vanlig org.slf4j.Logger slik:
 * `secure(log).info(msg)`
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
