package no.nav.rekrutteringsbistand.statistikk.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.utils.Environment
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import java.lang.RuntimeException
import java.sql.Connection

class Database(env: Environment) : DatabaseInterface {

    data class DbConf(val mountPath: String, val jdbcUrl: String)

    private val config = when (env.profil) {
        "dev" -> DbConf(
            mountPath = "postgresql/preprod-fss",
            jdbcUrl = "jdbc:postgresql://b27dbvl009.preprod.local:5432/rekrutteringsbistand-statistikk"
        )
        "prod" -> DbConf(
            mountPath = "postgresql/prod-fss",
            jdbcUrl = "jdbc:postgresql://A01DBVL011.adeo.no:5432/rekrutteringsbistand-statistikk"
        )
        else -> throw RuntimeException("Feil ved oppsett av database config, ukjent profil: ${env.profil}")
    }

    private val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

    init {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            minimumIdle = 1
            maximumPoolSize = 2
            driverClassName = "org.postgresql.Driver"
        }

        dataSource = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            config.mountPath,
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

