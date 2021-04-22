package no.nav.rekrutteringsbistand.statistikk.datakatalog

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

class DatakatalogScheduler(dataSource: DataSource, private val runnable: Runnable) {

    private val lockProvider = JdbcLockProvider(dataSource)
    private val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    private val runnableMedLås: TimerTask.() -> Unit = {
        lockingExecutor.executeWithLock(
            runnable,
            LockConfiguration(Instant.now(),"retry-lock", Duration.ofMinutes(10), Duration.ofMillis(0L))
        )
    }

    fun kjørPeriodisk() {
        fixedRateTimer(
            name = "Send hull i cv til Datakatalog periodisk",
            period = Duration.ofSeconds(60).toMillis(),
            action = runnableMedLås,
            initialDelay = Duration.ofSeconds(60).toMillis()
        )
    }

    fun kjørEnGangAsync(delay: Long = 0L) {
        val timerTask = timerTask(runnableMedLås)
        val timer = Timer("Send hull i cv til Datakatalog  én gang")
        timer.schedule(timerTask, delay)
    }
}
