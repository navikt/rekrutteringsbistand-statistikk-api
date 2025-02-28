package no.nav.statistikkapi.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.statistikkapi.Cluster
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import javax.sql.DataSource

private const val databaseNavn = "rekrutteringsbistand-statistikk-pg15"

class Database(cluster: Cluster) {

    private data class DbConf(val mountPath: String, val jdbcUrl: String)

    private val config = when (cluster) {
        Cluster.DEV_FSS -> DbConf(
            mountPath = "postgresql/preprod-fss",
            jdbcUrl = "jdbc:postgresql://b27dbvl033.preprod.local:5432/$databaseNavn"
        )
        Cluster.PROD_FSS -> DbConf(
            mountPath = "postgresql/prod-fss",
            jdbcUrl = "jdbc:postgresql://A01DBVL037.adeo.no:5432/$databaseNavn"
        )
        Cluster.LOKAL -> throw UnsupportedOperationException()
    }

    val dataSource: DataSource

    init {
        dataSource = opprettDataSource(role = "user")
        kjørFlywayMigreringer()
    }

    private fun opprettDataSource(role: String): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            minimumIdle = 1
            maximumPoolSize = 2
            driverClassName = "org.postgresql.Driver"
        }

        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            config.mountPath,
            "$databaseNavn-$role"
        )
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(opprettDataSource(role = "admin"))
            .initSql("SET ROLE \"$databaseNavn-admin\"")
            .load()
            .migrate()
    }
}
