package no.nav.rekrutteringsbistand.statistikk.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

//TODO: Are: Slettes
//fun startScheduler(dataSource: DataSource, runnable: Runnable) {
//    val lockProvider = JdbcLockProvider(dataSource)
//    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
//
//    fixedRateTimer(period = Duration.ofMinutes(1).toMillis()) {
//        lockingExecutor.executeWithLock(
//            runnable,
//            LockConfiguration("retry-lock", Duration.ofMinutes(10), Duration.ofSeconds(10))
//        )
//    }
//}

class KafkaTilDataverehusScheduler(private val dataSource: DataSource, private val runnable: Runnable) {

    fun executePeriodically() {
        val shedlockedRunnable = withShedlock(dataSource, runnable)
        fixedRateTimer(period = Duration.ofMinutes(1).toMillis(), action = shedlockedRunnable)
    }

    suspend fun executeOnceAsync(delay: Long = 0L) {
        val shedlockedRunnable = withShedlock(dataSource, runnable)
        val timerTask = timerTask(shedlockedRunnable)
        val timer = Timer("Send Kafka-mleding til Datavarehus")
        timer.schedule(timerTask, delay)
    }

    companion object {

        private fun withShedlock(dataSource: DataSource, runnable: Runnable): TimerTask.() -> Unit {
            val lockProvider = JdbcLockProvider(dataSource)
            val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)
            return {
                lockingExecutor.executeWithLock(
                    runnable,
                    LockConfiguration("retry-lock", Duration.ofMinutes(10), Duration.ofSeconds(10))
                )
            }
        }
    }
}