package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import no.nav.rekrutteringsbistand.statistikk.HentStatistikk
import no.nav.rekrutteringsbistand.statistikk.datakatalog.AlderDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.datakatalog.Aldersgruppe
import no.nav.rekrutteringsbistand.statistikk.datakatalog.hull.HullDatagrunnlag
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.SendtStatus.IKKE_SENDT
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Utfall.FATT_JOBBEN
import no.nav.rekrutteringsbistand.statistikk.kandidatutfall.Utfall.PRESENTERT
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class KandidatutfallRepository(private val dataSource: DataSource) {

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
                               $tidspunkt,
                               $hullICv,
                               $alder
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatutfall.aktørId)
                setString(2, kandidatutfall.utfall.name)
                setString(3, kandidatutfall.navIdent)
                setString(4, kandidatutfall.navKontor)
                setString(5, kandidatutfall.kandidatlisteId)
                setString(6, kandidatutfall.stillingsId)
                setTimestamp(7, Timestamp.valueOf(registrertTidspunkt))
                if (kandidatutfall.harHullICv != null) setBoolean(8, kandidatutfall.harHullICv) else setNull(8, 0)
                if (kandidatutfall.alder != null) setInt(9, kandidatutfall.alder) else setNull(9, 0)
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

    fun hentAntallPresentert(hentStatistikk: HentStatistikk): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2
                     WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktørId, $kandidatlisteid) as k2
                     
                WHERE k1.$navkontor = ? 
                  AND k1.$dbId = k2.maksId
                  AND (k1.$utfall = '${FATT_JOBBEN.name}' OR k1.$utfall = '${PRESENTERT.name}')
            """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(hentStatistikk.fraOgMed))
                setDate(2, Date.valueOf(hentStatistikk.tilOgMed))
                setString(3, hentStatistikk.navKontor)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater fra databasen")
            }
        }
    }

    fun hentAntallFåttJobben(hentStatistikk: HentStatistikk): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2
                     WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktørId, $kandidatlisteid) as k2
                     
                WHERE k1.$navkontor = ?
                  AND k1.$dbId = k2.maksId
                  AND k1.$utfall = '${FATT_JOBBEN.name}'
            """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(hentStatistikk.fraOgMed))
                setDate(2, Date.valueOf(hentStatistikk.tilOgMed))
                setString(3, hentStatistikk.navKontor)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidater som har fått jobben fra databasen")
            }
        }
    }

    fun hentAntallFåttJobben(harHull: Boolean?, fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(telleliste.*) FROM $kandidatutfallTabell telleliste,
                  (SELECT MIN($dbId) as minId FROM $kandidatutfallTabell tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon,
                    (SELECT senesteUtfallITidsromOgFåttJobben.$aktørId, senesteUtfallITidsromOgFåttJobben.$kandidatlisteid FROM $kandidatutfallTabell senesteUtfallITidsromOgFåttJobben,  
                        (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell senesteUtfallITidsrom
                        WHERE senesteUtfallITidsrom.$tidspunkt BETWEEN ? AND ?
                        GROUP BY senesteUtfallITidsrom.$aktørId, senesteUtfallITidsrom.$kandidatlisteid) as senesteUtfallITidsrom
                    WHERE senesteUtfallITidsromOgFåttJobben.${dbId} = senesteUtfallITidsrom.maksId
                    AND senesteUtfallITidsromOgFåttJobben.$utfall = '${FATT_JOBBEN.name}') as senesteUtfallITidsromOgFåttJobben                  
                  WHERE tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$aktørId = senesteUtfallITidsromOgFåttJobben.$aktørId
                  AND tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$kandidatlisteid = senesteUtfallITidsromOgFåttJobben.$kandidatlisteid
                  GROUP BY tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$aktørId, tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$kandidatlisteid) as tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon
                WHERE  telleliste.$dbId = tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.minId
                AND telleliste.$hullICv
            """.trimIndent() + if (harHull != null) " = ?" else " IS NULL"
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
                if (harHull != null) setBoolean(3, harHull)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidater med harHull=$harHull som har fått jobben fra databasen")
            }
        }
    }

    fun hentAntallPresentert(harHull: Boolean?, fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2
                     WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktørId, $kandidatlisteid) as k2
                     
                WHERE  k1.$dbId = k2.maksId
                  AND (k1.$utfall = '${FATT_JOBBEN.name}' OR k1.$utfall = '${PRESENTERT.name}')
                  AND k1.$hullICv
            """.trimIndent() + if (harHull != null) " = ?" else " IS NULL"
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
                if (harHull != null) setBoolean(3, harHull)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater fra databasen")
            }
        }
    }

    fun hentAntallPresentert(aldersgruppe: Aldersgruppe, fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2
                     WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktørId, $kandidatlisteid) as k2
                     
                WHERE  k1.$dbId = k2.maksId
                  AND (k1.$utfall = '${FATT_JOBBEN.name}' OR k1.$utfall = '${PRESENTERT.name}')
                  AND (k1.$alder BETWEEN ? AND ?)
            """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
                setInt(3, aldersgruppe.min)
                setInt(4, aldersgruppe.max)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater for aldersgruppe $aldersgruppe fra databasen")
            }
        }
    }

    fun hentAntallFåttJobben(aldersgruppe: Aldersgruppe, fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(k1.*) FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell k2
                    WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktørId, $kandidatlisteid) as k2
                     
                WHERE  k1.$dbId = k2.maksId
                  AND k1.$utfall = '${FATT_JOBBEN.name}'
                  AND (k1.$alder BETWEEN ? AND ?)
            """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(tilOgMed))
                setInt(3, aldersgruppe.min)
                setInt(4, aldersgruppe.max)
            }.executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall kandidater med aldersgruppe $aldersgruppe som har fått jobben fra databasen")
            }
        }
    }

    fun hentHullDatagrunnlag(datoer: List<LocalDate>): HullDatagrunnlag {
        return HullDatagrunnlag(
            datoer.flatMap { dag ->
                listOf(true, false, null).map { harHull ->
                    (dag to harHull) to hentAntallPresentert(harHull, dag, dag.plusDays(1))
                }
            }.toMap(),
            datoer.flatMap { dag ->
                listOf(true, false, null).map { harHull ->
                    (dag to harHull) to hentAntallFåttJobben(harHull, dag, dag.plusDays(1))
                }
            }.toMap()
        )
    }

    fun hentAlderDatagrunnlag(datoer: List<LocalDate>): AlderDatagrunnlag {
        return AlderDatagrunnlag(
            datoer.flatMap { dag ->
                Aldersgruppe.values().map { aldersgruppe ->
                    (dag to aldersgruppe) to hentAntallPresentert(aldersgruppe, dag, dag.plusDays(1))
                }
            }.toMap(),
            datoer.flatMap { dag ->
                Aldersgruppe.values().map { aldersgruppe ->
                    (dag to aldersgruppe) to hentAntallFåttJobben(aldersgruppe, dag, dag.plusDays(1))
                }
            }.toMap()
        )
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
        const val hullICv = "hull_i_cv"
        const val sendtStatus = "sendt_status"
        const val antallSendtForsøk = "antall_sendt_forsok"
        const val sisteSendtForsøk = "siste_sendt_forsok"
        const val alder = "alder"

        fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
            Kandidatutfall(
                dbId = resultSet.getLong(KandidatutfallRepository.dbId),
                aktorId = resultSet.getString(KandidatutfallRepository.aktørId),
                utfall = Utfall.valueOf(resultSet.getString(KandidatutfallRepository.utfall)),
                navIdent = resultSet.getString(KandidatutfallRepository.navident),
                navKontor = resultSet.getString(KandidatutfallRepository.navkontor),
                kandidatlisteId = UUID.fromString(resultSet.getString(KandidatutfallRepository.kandidatlisteid)),
                stillingsId = UUID.fromString(resultSet.getString(KandidatutfallRepository.stillingsid)),
                hullICv = if(resultSet.getObject(KandidatutfallRepository.hullICv) == null)  null  else resultSet.getBoolean(KandidatutfallRepository.hullICv),
                tidspunkt = resultSet.getTimestamp(KandidatutfallRepository.tidspunkt).toLocalDateTime(),
                antallSendtForsøk = resultSet.getInt(KandidatutfallRepository.antallSendtForsøk),
                sendtStatus = SendtStatus.valueOf(resultSet.getString(KandidatutfallRepository.sendtStatus)),
                sisteSendtForsøk = resultSet.getTimestamp(KandidatutfallRepository.sisteSendtForsøk)?.toLocalDateTime(),
                alder = if(resultSet.getObject(KandidatutfallRepository.alder) == null) null else resultSet.getInt(KandidatutfallRepository.alder)
            )
    }
}
