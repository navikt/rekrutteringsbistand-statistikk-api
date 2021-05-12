package statistikkapi.stillinger

import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

class StillingRepository(private val dataSource: DataSource) {


    fun lagreStilling(stilling: ElasticSearchStilling): Long {
         return dataSource.connection.use {
            it.prepareStatement(
                """INSERT into $stillingtabell (
                               $uuid,
                               $opprettet,
                               $publisert,
                               $inkluderingsmuligheter,
                               $prioriterteMålgrupper,
                               $tiltakEllerVirkemidler,
                               $tidspunkt    
                ) VALUES (?, ?, ?, ?, ?, ?, ?)"""
            , Statement.RETURN_GENERATED_KEYS).run {
                setString(1, stilling.uuid)
                setTimestamp(2, Timestamp.valueOf(stilling.opprettet))
                setTimestamp(3, Timestamp.valueOf(stilling.publisert))
                setString(4, stilling.inkluderingsmuligheter.joinToString(listeseparator))
                setString(5, stilling.prioriterteMålgrupper.joinToString(listeseparator))
                setString(6, stilling.tiltakEllerEllerVirkemidler.joinToString(listeseparator))
                setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
                executeUpdate()

                generatedKeys.next()
                generatedKeys.getLong(1)
            }
        }
    }

    fun hentStilling(stillingUuid: String): Stilling? =
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                    SELECT * from $stillingtabell
                    WHERE $uuid = ?
                """.trimIndent()
            ).apply {
                setString(1, stillingUuid)
            }.executeQuery()

            if (resultSet.next()) return resultSet.konverterTilStilling() else null
        }


    fun ResultSet.konverterTilStilling() = Stilling(
        id = getLong(dbId),
        uuid = getString(uuid),
        opprettet = getTimestamp(opprettet).toLocalDateTime(),
        publisert = getTimestamp(publisert).toLocalDateTime(),
        inkluderingsmuligheter = if (getString(inkluderingsmuligheter).isBlank()) emptyList() else getString(inkluderingsmuligheter).split(
            listeseparator).map { InkluderingTag.valueOf(it) },
        prioriterteMålgrupper = if (getString(prioriterteMålgrupper).isBlank()) emptyList() else getString(prioriterteMålgrupper).split(listeseparator).map { PrioriterteMålgrupperTag.valueOf(it) },
        tiltakEllerEllerVirkemidler = if (getString(tiltakEllerVirkemidler).isBlank()) emptyList() else getString(tiltakEllerVirkemidler).split(
            listeseparator).map { TiltakEllerVirkemiddelTag.valueOf(it) },
        tidspunkt = getTimestamp(tidspunkt).toLocalDateTime()

    )

    companion object {
        const val stillingtabell = "stilling"
        const val dbId = "id"
        const val uuid = "uuid"
        const val opprettet = "opprettet"
        const val publisert = "publisert"
        const val inkluderingsmuligheter = "inkluderingsmuligheter"
        const val prioriterteMålgrupper = "prioritertemålgrupper"
        const val tiltakEllerVirkemidler = "tiltakellervirkemidler"
        const val tidspunkt = "tidspunkt"
        const val listeseparator = ";"
    }
}
