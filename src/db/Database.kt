package no.nav.rekrutteringsbistand.statistikk.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.flywaydb.core.Flyway
import java.util.*


val dbjdbcUrl = "jdbc:h2:mem:rekrutteringsbistand-statistikk";
val dbusername = "sa"
val dbpassword = ""

class Database() {
    private val dataSource: HikariDataSource

    val connection: Connection
        get() = dataSource.connection

    init {

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbjdbcUrl
            username = dbusername
            password = dbpassword
            maximumPoolSize = 3
        })
        dataSource.validate()

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate()
    }
}
