package no.nav.statistikkapi.stillinger

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.minidev.json.JSONArray
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

class StillingRepository(private val dataSource: DataSource) {


    fun lagreStilling(stilling: ElasticSearchStilling) {
        dataSource.connection.use {
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
            ).run {
                setString(1, stilling.uuid)
                setTimestamp(2, Timestamp.valueOf(stilling.opprettet))
                setTimestamp(3, Timestamp.valueOf(stilling.publisert))
                setString(4, stilling.inkluderingsmuligheter.somJSONArray())
                setString(5, stilling.prioriterteMålgrupper.somJSONArray())
                setString(6, stilling.tiltakEllerEllerVirkemidler.somJSONArray())
                setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
                executeUpdate()
            }
        }
    }

    fun hentNyesteStilling(stillingUuid: String): Stilling? =
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                    SELECT tabell.* from $stillingtabell tabell,
                        (SELECT max($tidspunkt) as maksTidsPunkt, $uuid from $stillingtabell
                        WHERE $uuid = ?
                        GROUP BY $uuid) as stillingMaksTabell
                    WHERE tabell.$tidspunkt = stillingMaksTabell.maksTidsPunkt
                    AND tabell.$uuid = stillingMaksTabell.$uuid
                """.trimIndent()
            ).apply {
                setString(1, stillingUuid)
            }.executeQuery()

            if (resultSet.next()) return resultSet.konverterTilStilling() else null
        }


    fun ResultSet.konverterTilStilling() = Stilling(
        uuid = getString(uuid),
        opprettet = getTimestamp(opprettet).toLocalDateTime(),
        publisert = getTimestamp(publisert).toLocalDateTime(),
        inkluderingsmuligheter = listFromJSONArray(
            inkluderingsmuligheter,
            object : TypeReference<List<InkluderingTag>>() {}),
        prioriterteMålgrupper = listFromJSONArray(
            prioriterteMålgrupper,
            object : TypeReference<List<PrioriterteMålgrupperTag>>() {}),
        tiltakEllerVirkemidler = listFromJSONArray(
            tiltakEllerVirkemidler,
            object : TypeReference<List<TiltakEllerVirkemiddelTag>>() {}),
        tidspunkt = getTimestamp(tidspunkt).toLocalDateTime()
    )

    companion object {
        const val stillingtabell = "stilling"
        const val uuid = "uuid"
        const val opprettet = "opprettet"
        const val publisert = "publisert"
        const val inkluderingsmuligheter = "inkluderingsmuligheter"
        const val prioriterteMålgrupper = "prioritertemålgrupper"
        const val tiltakEllerVirkemidler = "tiltakellervirkemidler"
        const val tidspunkt = "tidspunkt"
    }
}

private fun List<*>.somJSONArray() = JSONArray.toJSONString(this)

private fun <T> ResultSet.listFromJSONArray(columnKey: String, typeReference: TypeReference<List<T>>) =
    ObjectMapper().readValue(this.getString(columnKey), typeReference)
