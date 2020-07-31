package no.nav.rekrutteringsbistand.statistikk.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

fun startScheduler(dataSource: DataSource, runnable: Runnable) {
    val lockProvider = JdbcLockProvider(dataSource)
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    fixedRateTimer(period = Duration.ofMinutes(1).toMillis()) {
        lockingExecutor.executeWithLock(
            runnable,
            LockConfiguration("retry-lock", Duration.ofMinutes(10), Duration.ofSeconds(10))
        )
    }
}

