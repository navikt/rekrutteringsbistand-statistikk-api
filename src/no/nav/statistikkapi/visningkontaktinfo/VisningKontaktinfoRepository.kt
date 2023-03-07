package no.nav.statistikkapi.visningkontaktinfo

import java.sql.Timestamp
import javax.sql.DataSource

class VisningKontaktinfoRepository(private val dataSource: DataSource) {

    fun lagre(visningKontaktinfo: VisningKontaktinfo) {
        dataSource.connection.use {
            it.prepareStatement(
                """insert into visning_kontaktinfo (aktør_id, stilling_id, tidspunkt) values (?, ?, ?)"""
            ).apply {
                setString(1, visningKontaktinfo.aktørId)
                setObject(2, visningKontaktinfo.stillingId)
                setTimestamp(3, Timestamp(visningKontaktinfo.tidspunkt.toInstant().toEpochMilli()))
                executeUpdate()
            }
        }
    }
}

