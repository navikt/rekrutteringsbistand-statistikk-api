package statistikkapi.stillinger

import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

// TODO: Skal én stilling lagres flere ganger?
// Hva skal logikken være? Skal vi oppdatere hvis den finnes fra før av?

class StillingRepository(private val dataSource: DataSource) {


    fun lagreStilling(stilling: ElasticSearchStilling): Long {
         return dataSource.connection.use {
            it.prepareStatement(
                """INSERT into $stillingtabell (
                               $uuid,
                               $publiseringsdato,
                               $inkluderingsmuligheter,
                               $prioriterteMålgrupper,
                               $tiltakEllerVirkemidler,
                               $tidspunkt    
                ) VALUES (?, ?, ?, ?, ?, ?)"""
            , Statement.RETURN_GENERATED_KEYS).run {
                setString(1, stilling.uuid)
                setTimestamp(2, Timestamp.valueOf(stilling.publisert))
                setString(3, stilling.inkluderingsmuligheter.joinToString(listeseparator))
                setString(4, stilling.prioriterteMålgrupper.joinToString(listeseparator))
                setString(5, stilling.tiltakEllerEllerVirkemidler.joinToString(listeseparator))
                setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()))
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
        publisert = getTimestamp(publiseringsdato).toLocalDateTime(),
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
        const val publiseringsdato = "publiseringsdato"
        const val inkluderingsmuligheter = "inkluderingsmuligheter"
        const val prioriterteMålgrupper = "prioritertemålgrupper"
        const val tiltakEllerVirkemidler = "tiltakellervirkemidler"
        const val tidspunkt = "tidspunkt"
        const val listeseparator = ";"
    }
}
