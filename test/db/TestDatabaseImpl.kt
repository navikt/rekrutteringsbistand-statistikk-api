package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.db.Database
import org.flywaydb.core.Flyway
import java.sql.Connection
import javax.sql.DataSource

class TestDatabaseImpl : Database {

    override val dataSource: DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
            isAutoCommit = false
            validate()
        })

    override val connection: Connection
        get() = dataSource.connection

    init {
        kjørFlywayMigreringer()
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

}
