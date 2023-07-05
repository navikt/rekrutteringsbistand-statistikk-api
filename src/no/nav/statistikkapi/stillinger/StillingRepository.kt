package no.nav.statistikkapi.stillinger

import no.nav.statistikkapi.logWithoutClassname
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import javax.sql.DataSource

class StillingRepository(private val dataSource: DataSource) {


    fun lagreStilling(stillingsuuid: String, stillingskategori: Stillingskategori?) {
        try {
            dataSource.connection.use {
                it.prepareStatement(
                    """INSERT INTO $stillingtabell (
                               $uuidLabel,
                               $stillingskategoriLabel
                ) VALUES (?, ?)"""
                ).run {
                    setString(1, stillingsuuid)
                    setString(2, stillingskategori?.name)
                    executeUpdate()
                }
            }
        } catch (e: SQLIntegrityConstraintViolationException) {
            // TODO: Brukes bare for test på grunn av H2/PSQL
        } catch (e: PSQLException) {
            if (e.message?.contains("ERROR: duplicate key value violates unique constraint \"stilling_pkey\"") != true) {
                throw e
            }
        }
    }

    fun hentStilling(stillingsId: UUID): Stilling? = hentStilling(stillingsId.toString())


    private fun hentStilling(stillingUuid: String): Stilling? =
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                    SELECT $uuidLabel, $stillingskategoriLabel from $stillingtabell
                    WHERE $uuidLabel = ?
                """.trimIndent()
            ).apply {
                setString(1, stillingUuid)
            }.executeQuery()

            if (resultSet.next()) return konverterTilStilling(resultSet) else null
        }

    companion object {
        const val stillingtabell = "stilling"
        const val uuidLabel = "uuid"
        const val stillingskategoriLabel = "stillingskategori"
    }
}

fun konverterTilStilling(rs: ResultSet) = Stilling(
    uuid = rs.getString(StillingRepository.uuidLabel),
    stillingskategori = Stillingskategori.fraNavn(rs.getString(StillingRepository.stillingskategoriLabel).also {
        if (it == null) logWithoutClassname.info("Stillingskategori var null i databasen for stillingsID ${StillingRepository.uuidLabel}. Tolker det som at dette er en vanlig stilling og bruker verdien ${Stillingskategori.STILLING} videre istedenfor null")
    })
)
