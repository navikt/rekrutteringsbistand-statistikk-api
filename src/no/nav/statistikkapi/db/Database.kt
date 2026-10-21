package no.nav.statistikkapi.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class Database(env: Map<String, String> = System.getenv()) {

    val dataSource: DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = env["DB_JDBC_URL"] ?: throw IllegalStateException("Mangler DB_JDBC_URL som systemvariabel")
            minimumIdle = 1
            maximumPoolSize = 2
            driverClassName = "org.postgresql.Driver"
            initializationFailTimeout = 5000
            validate()
        }
    )

    init {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}
