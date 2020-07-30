package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.db.Database
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.aktørId
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.kandidatlisteid
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.kandidatutfallTabell
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.navident
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.navkontor
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.stillingsid
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.tidspunkt
import no.nav.rekrutteringsbistand.statistikk.db.Repository.Companion.utfall
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Kandidatutfall
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet
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
