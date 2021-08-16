package statistikkapi.datakatalog

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import statistikkapi.log
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

class DatakatalogScheduler(dataSource: DataSource, private val runnable: Runnable) {

    private val lockProvider = JdbcLockProvider(dataSource)
    private val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    private val runnableMedLås: TimerTask.() -> Unit = {
        lockingExecutor.executeWithLock(
            SwallowAndLogExceptions(runnable),
            LockConfiguration(Instant.now(),"retry-lock", Duration.ofMinutes(10), Duration.ofMillis(0L))
        )
    }

    fun kjørPeriodisk() {
        fixedRateTimer(
            name = "Send statistikk til Datakatalog periodisk",
            period = Duration.ofSeconds(60).toMillis(),
            action = runnableMedLås,
            initialDelay = Duration.ofSeconds(60).toMillis()
        )
    }
    private class SwallowAndLogExceptions(private val runnable: Runnable):Runnable {
        override fun run() {
            try {
                runnable.run()
            } catch (e:Exception) {
                log.error("Feil ved sending av statistikk til datavarehus", e)
            }
        }
    }
}
