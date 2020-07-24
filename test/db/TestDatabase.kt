package db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.rekrutteringsbistand.statistikk.db.DatabaseInterface
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.*
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet

class TestDatabase : DatabaseInterface {

    private val dataSource: HikariDataSource = HikariDataSource(
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

    fun hentUtfall(): List<Kandidatutfall> {
        connection.use {
            val resultSet = it.prepareStatement("SELECT * FROM $kandidatutfallTabell").executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }

    private fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
        Kandidatutfall(
            aktorId = resultSet.getString(aktørId),
            utfall = resultSet.getString(utfall),
            navIdent = resultSet.getString(navident),
            navKontor = resultSet.getString(navkontor),
            kandidatlisteId = resultSet.getString(kandidatlisteid),
            stillingsId = resultSet.getString(stillingsid),
            tidspunkt = resultSet.getTimestamp(tidspunkt).toLocalDateTime()
        )

    fun slettAlleUtfall() {
        connection.use { it.prepareStatement("DELETE FROM $kandidatutfallTabell").execute() }
    }
}
