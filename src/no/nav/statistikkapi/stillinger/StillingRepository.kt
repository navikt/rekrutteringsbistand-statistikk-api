package no.nav.statistikkapi.stillinger

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.minidev.json.JSONArray
import no.nav.statistikkapi.log
import org.postgresql.util.PSQLException
import java.sql.ResultSet
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.Timestamp
import java.time.LocalDateTime
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
        }catch (e: SQLIntegrityConstraintViolationException) {
            // TODO: Brukes bare for test på grunn av H2/PSQL
        }
        catch (e: PSQLException) {
            if(e.message?.contains("ERROR: duplicate key value violates unique constraint \"stilling_pkey\"") != true){
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

            if (resultSet.next()) return resultSet.konverterTilStilling() else null
        }

    fun ResultSet.konverterTilStilling() = Stilling(
        uuid = getString(uuidLabel),
        stillingskategori = Stillingskategori.fraDatabase(getString(stillingskategoriLabel).also {
            if (it == null) log.info("Stillingskategori var null i databasen for stillingsID $uuidLabel. Tolker det som at dette er en vanlig stilling og bruker verdien ${Stillingskategori.STILLING} videre istedenfor null")
        })
    )

    companion object {
        const val stillingtabell = "stilling"
        const val uuidLabel = "uuid"
        const val stillingskategoriLabel = "stillingskategori"
    }
}