package no.nav.rekrutteringsbistand.statistikk.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import java.sql.Connection

class Database(env: Environment) : DatabaseInterface {
    private val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

    init {
        val mountPath: String =
            when (env.profil) {
                "dev" -> "postgresql/preprod-fss"
                "prod" -> "postgresql/prod-fss"
                else -> throw RuntimeException("Vault mountPath variabel må være satt")
            }

        val dbJdbcUrl: String =
            when (env.profil) {
                "dev" -> "jdbc:postgresql://b27dbvl009.preprod.local:5432/rekrutteringsbistand-statistikk"
                "prod" -> "jdbc:postgresql://A01DBVL011.adeo.no:5432/rekrutteringsbistand-statistikk"
                else -> throw RuntimeException("Database JDBC-URL må være satt")
            }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbJdbcUrl
            minimumIdle = 1
            maximumPoolSize = 2
        }

        dataSource = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            mountPath,
            "rekrutteringsbistand-statistikk-user"
        )

        kjørFlywayMigreringer()
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .initSql("SET ROLE \"rekrutteringsbistand-statistikk-admin\"")
            .load()
            .migrate()
    }
}

interface DatabaseInterface {
    val connection: Connection
}

