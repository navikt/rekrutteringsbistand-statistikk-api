package no.nav.statistikkapi.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

class KafkaTilDataverehusScheduler(
    dataSource: DataSource,
    private val sendKafkaMeldingFraUtfall: Runnable,
    sendKafkaMeldingFraTiltak: Runnable
) {

    private val lockProvider = JdbcLockProvider(dataSource)
    private val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    private val sendKafkaMeldingFraUtfallMedLås: TimerTask.() -> Unit = {
        lockingExecutor.executeWithLock(
            sendKafkaMeldingFraUtfall,
            LockConfiguration(Instant.now(), "retry-lock", Duration.ofMinutes(10), Duration.ofMillis(0L))
        )
    }

    private val sendKafkaMeldingFraTiltakMedLås: TimerTask.() -> Unit = {
        lockingExecutor.executeWithLock(
            sendKafkaMeldingFraTiltak,
            LockConfiguration(Instant.now(), "retry-lock", Duration.ofMinutes(10), Duration.ofMillis(0L))
        )
    }

    fun kjørPeriodisk() {
        fixedRateTimer(
            name = "Send Kafka-melding fra tiltak til Datavarehus periodisk",
            period = Duration.ofMinutes(10).toMillis(),
            action = sendKafkaMeldingFraUtfallMedLås
        )

        fixedRateTimer(
            name = "Send Kafka-melding fra tiltak til Datavarehus periodisk",
            period = Duration.ofMinutes(10).toMillis(),
            action = sendKafkaMeldingFraTiltakMedLås
        )
    }

    fun kjørEnGangAsync(delay: Long = 0L) {

        Timer("Send Kafka-melding utfall til Datavarehus én gang")
            .schedule(
                timerTask(sendKafkaMeldingFraUtfallMedLås), delay
            )

        Timer("Send Kafka-melding tiltak til Datavarehus én gang")
            .schedule(
                timerTask(sendKafkaMeldingFraTiltakMedLås), delay
            )
    }
}
