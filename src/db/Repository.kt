package no.nav.rekrutteringsbistand.statistikk.db

import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.IKKE_SENDT
import no.nav.rekrutteringsbistand.statistikk.db.Utfall.*
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.OpprettKandidatutfall
import java.lang.RuntimeException
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {

    fun lagreUtfall(kandidatutfall: OpprettKandidatutfall, registrertTidspunkt: LocalDateTime) {
        dataSource.connection.use {
            it.prepareStatement(
                """INSERT INTO $kandidatutfallTabell (
                               $aktørId,
                               $utfall,
                               $navident,
                               $navkontor,
                               $kandidatlisteid,
                               $stillingsid,
                               $tidspunkt
                ) VALUES (?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatutfall.aktørId)
                setString(2, kandidatutfall.utfall)
                setString(3, kandidatutfall.navIdent)
                setString(4, kandidatutfall.navKontor)
                setString(5, kandidatutfall.kandidatlisteId)
                setString(6, kandidatutfall.stillingsId)
                setTimestamp(7, Timestamp.valueOf(registrertTidspunkt))
                executeUpdate()
            }
        }
    }

    fun registrerSendtForsøk(utfall: Kandidatutfall) {
        dataSource.connection.use {
            it.prepareStatement(
                """UPDATE $kandidatutfallTabell
                      SET $antallSendtForsøk = ?,
                          $sisteSendtForsøk = ?
                    WHERE $dbId = ?"""
            ).apply {
                setInt(1, utfall.antallSendtForsøk + 1)
                setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                setLong(3, utfall.dbId)
                executeUpdate()
            }
        }
    }

    fun registrerSomSendt(utfall: Kandidatutfall) {
        dataSource.connection.use {
            it.prepareStatement(
                """UPDATE $kandidatutfallTabell
                      SET $sendtStatus = ?
                    WHERE $dbId = ?"""
            ).apply {
                setString(1, SendtStatus.SENDT.name)
                setLong(2, utfall.dbId)
                executeUpdate()
            }
        }
    }

    fun hentUsendteUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM $kandidatutfallTabell WHERE $sendtStatus = '${IKKE_SENDT.name}' ORDER BY $dbId ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }

    fun hentAntallPresentert(fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        // Vi teller presentert hvis nyeste registrering for et aktørId-kandidatlisteId-par er PRESENTERT eller FATT_JOBBEN
        dataSource.connection.use {
            val resultSet = it.prepareStatement("""
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2 GROUP BY $aktørId, $kandidatlisteid) as k2
                  WHERE
                        k1.$dbId = k2.maksId
                    AND (k1.$utfall = '${FATT_JOBBEN.name}' OR k1.$utfall = '${PRESENTERT.name}')
                    AND k1.$tidspunkt BETWEEN ? AND ?
            """.trimIndent()).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater fra databasen")
            }
        }
    }

    fun hentAntallFåttJobben(fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        // Vi teller PRESENTERT hvis nyeste registrering for et aktørId-kandidatlisteId-par er PRESENTERT hvis vi filtrerer bort FATT_JOBBEN
        dataSource.connection.use {
            val resultSet = it.prepareStatement("""
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2 GROUP BY $aktørId, $kandidatlisteid) as k2
                  WHERE
                        k1.$dbId = k2.maksId
                    AND k1.$utfall = '${FATT_JOBBEN.name}'
                    AND k1.$tidspunkt BETWEEN ? AND ?
            """.trimIndent()).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidater som har fått jobben fra databasen")
            }
        }
    }

    companion object {
        const val dbId = "id"
        const val kandidatutfallTabell = "kandidatutfall"
        const val aktørId = "aktorid"
        const val utfall = "utfall"
        const val navident = "navident"
        const val navkontor = "navkontor"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val tidspunkt = "tidspunkt"
        const val sendtStatus = "sendt_status"
        const val antallSendtForsøk = "antall_sendt_forsok"
        const val sisteSendtForsøk = "siste_sendt_forsok"
    }
}
