package no.nav.rekrutteringsbistand.statistikk.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.Cluster
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import java.sql.Connection

class Database(cluster: Cluster) : DatabaseInterface {

    data class DbConf(val mountPath: String, val jdbcUrl: String)

    private val config = when (cluster) {
        Cluster.DEV_FSS -> DbConf(
            mountPath = "postgresql/preprod-fss",
            jdbcUrl = "jdbc:postgresql://b27dbvl009.preprod.local:5432/rekrutteringsbistand-statistikk"
        )
        Cluster.PROD_FSS -> DbConf(
            mountPath = "postgresql/prod-fss",
            jdbcUrl = "jdbc:postgresql://A01DBVL011.adeo.no:5432/rekrutteringsbistand-statistikk"
        )
    }

    private val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

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
            "rekrutteringsbistand-statistikk-$role"
        )
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(opprettDataSource(role = "admin"))
            .initSql("SET ROLE \"rekrutteringsbistand-statistikk-admin\"")
            .load()
            .migrate()
    }
}

interface DatabaseInterface {
    val connection: Connection
}

