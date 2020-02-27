package no.nav.rekrutteringsbistand.statistikk.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.utils.Log
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.time.LocalDateTime


class Database {
    private val dataSource: HikariDataSource

    private val jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    private val username = "sa"
    private val password = ""

    val connection: Connection
        get() = dataSource.connection

    init {
        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .load()
                .migrate()

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = this@Database.jdbcUrl
            username = this@Database.username
            password = this@Database.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            maxLifetime = 300000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })

        connection.use { connection ->
            connection.prepareStatement("""
                INSERT INTO kandidatutfall
                VALUES (1, '123123123', 'FÅTT_JOBBEN', 'X123123', '1234', '${LocalDateTime.now()}')
            """).execute()
            connection.commit()
        }

        dataSource.connection.use { connection ->
            val rs = connection.prepareStatement("SELECT * FROM kandidatutfall").executeQuery()
            while (rs.next()) {
                val kandidatutfall = Kandidatutfall(
                        aktørId = rs.getString("aktorid"),
                        utfall = rs.getString("utfall"),
                        navIdent = rs.getString("navident"),
                        enhetsnr = rs.getString("enhetsnr"),
                        tidspunkt = LocalDateTime.parse(rs.getString("tidspunkt").replace(' ', 'T'))
                )
                Log.info(kandidatutfall.toString())
            }
            connection.commit()
        }
    }
}
