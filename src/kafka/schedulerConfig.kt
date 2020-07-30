package no.nav.rekrutteringsbistand.statistikk.kafka

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.rekrutteringsbistand.statistikk.db.Database
import java.time.Duration
import kotlin.concurrent.fixedRateTimer

fun startScheduler(database: Database, runnable: Runnable) {
    val lockingDataSource = database.dataSource
    val lockProvider = JdbcLockProvider(lockingDataSource)
    val lockingExecutor = DefaultLockingTaskExecutor(lockProvider)

    fixedRateTimer(period = Duration.ofMinutes(1).toMillis()) {
        lockingExecutor.executeWithLock(
            runnable,
            LockConfiguration("retry-lock", Duration.ofMinutes(10), Duration.ofSeconds(10))
        )
    }
}

