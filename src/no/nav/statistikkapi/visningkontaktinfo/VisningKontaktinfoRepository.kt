package no.nav.statistikkapi.visningkontaktinfo

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class VisningKontaktinfoRepository(private val dataSource: DataSource) {

    fun lagre(aktørId: String, stillingsId: UUID, tidspunkt: ZonedDateTime) {
        dataSource.connection.use {
            it.prepareStatement(
                """insert into visning_kontaktinfo (aktør_id, stilling_id, tidspunkt) values (?, ?, ?)"""
            ).apply {
                setString(1, aktørId)
                setObject(2, stillingsId)
                setTimestamp(3, Timestamp(tidspunkt.toInstant().toEpochMilli()))
                executeUpdate()
            }
        }
    }

    fun harAlleredeBlittLagret(aktørId: String, stillingsId: UUID, tidspunkt: ZonedDateTime): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select 1 from visning_kontaktinfo 
                where aktør_id = ?
                    and stilling_id = ?
                    and tidspunkt = ?
            """.trimIndent()
            ).apply {
                setString(1, aktørId)
                setObject(2, stillingsId)
                setTimestamp(3, Timestamp(tidspunkt.toInstant().toEpochMilli()))
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }
}

