package no.nav.rekrutteringsbistand.statistikk.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

class KafkaTilDataverehusScheduler(dataSource: DataSource, private val runnable: Runnable) {

    private val lockProvider = JdbcLockProvider(dataSource)
    private val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    private val runnableMedLås: TimerTask.() -> Unit = {
        lockingExecutor.executeWithLock(
            runnable,
            LockConfiguration("retry-lock", Duration.ofMinutes(10), Duration.ofMillis(0L))
        )
    }

    fun kjørPeriodisk() {
        fixedRateTimer(
            name = "Send Kafka-meleding til Datavarehus periodisk",
            period = Duration.ofMinutes(1).toMillis(),
            action = runnableMedLås
        )
    }

    fun kjørEnGangAsync(delay: Long = 0L) {
        val timerTask = timerTask(runnableMedLås)
        val timer = Timer("Send Kafka-meleding til Datavarehus én gang")
        timer.schedule(timerTask, delay)
    }
}
