package no.nav.statistikkapi.kandidatutfall

import no.nav.statistikkapi.HentStatistikk
import no.nav.statistikkapi.kandidatutfall.SendtStatus.IKKE_SENDT
import no.nav.statistikkapi.kandidatutfall.Utfall.FATT_JOBBEN
import no.nav.statistikkapi.kandidatutfall.Utfall.PRESENTERT
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class KandidatutfallRepository(private val dataSource: DataSource) {

    fun lagreUtfall(kandidatutfall: OpprettKandidatutfall) {

        dataSource.connection.use {
            it.prepareStatement(
                """INSERT INTO $kandidatutfallTabell (
                               $aktorid,
                               $utfall,
                               $navident,
                               $navkontor,
                               $kandidatlisteid,
                               $stillingsid,
                               $synligKandidat,
                               $tidspunkt,
                               $hullICv,
                               $alder,
                               $tilretteleggingsbehov,
                               $innsatsbehov,
                               $hovedmål
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            ).apply {
                setString(1, kandidatutfall.aktørId)
                setString(2, kandidatutfall.utfall.name)
                setString(3, kandidatutfall.navIdent)
                setString(4, kandidatutfall.navKontor)
                setString(5, kandidatutfall.kandidatlisteId)
                setString(6, kandidatutfall.stillingsId)
                setBoolean(7, kandidatutfall.synligKandidat)
                setTimestamp(8, Timestamp.valueOf(kandidatutfall.tidspunktForHendelsen.toLocalDateTime()))
                if (kandidatutfall.harHullICv != null) setBoolean(9, kandidatutfall.harHullICv) else setNull(9, 0)
                if (kandidatutfall.alder != null) setInt(10, kandidatutfall.alder) else setNull(10, 0)
                setString(
                    11,
                    kandidatutfall.tilretteleggingsbehov.joinToString(separator = tilretteleggingsbehovdelimiter)
                )
                setString(12, kandidatutfall.innsatsbehov)
                setString(13, kandidatutfall.hovedmål)
                executeUpdate()
            }
        }
    }

    fun kandidatutfallAlleredeLagret(kandidatutfall: OpprettKandidatutfall): Boolean {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select 1 from $kandidatutfallTabell 
                where $aktorid = ?
                    and $kandidatlisteid = ?
                    and $utfall = ?
                    and $tidspunkt = ?
                    and $navident = ?
            """.trimIndent()
            ).apply {
                setString(1, kandidatutfall.aktørId)
                setString(2, kandidatutfall.kandidatlisteId)
                setString(3, kandidatutfall.utfall.toString())
                setTimestamp(4, Timestamp.valueOf(kandidatutfall.tidspunktForHendelsen.toLocalDateTime()))
                setString(5, kandidatutfall.navIdent)
                val resultSet = executeQuery()
                return resultSet.next()
            }
        }
    }

    fun hentSisteUtfallForKandidatIKandidatliste(aktørId: String, kandidatlisteId: String): Kandidatutfall? {
        dataSource.connection.use {
            it.prepareStatement(
                """
                select * from $kandidatutfallTabell 
                where $aktorid = ?
                    and $kandidatlisteid = ?
                    ORDER BY $dbId DESC limit 1
            """.trimIndent()
            ).apply {
                setString(1, aktørId)
                setString(2, kandidatlisteId)
                val resultSet = executeQuery()
                return if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                    else null
            }

        }
    }

    fun hentSisteUtfallForKandidatIKandidatliste(kandidatutfall: OpprettKandidatutfall): Utfall? =
        hentSisteUtfallForKandidatIKandidatliste(kandidatutfall.aktørId, kandidatutfall.kandidatlisteId)?.utfall

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
                SELECT COUNT(unike_presenteringer_per_person_og_liste.*) FROM (
                    SELECT DISTINCT k1.$aktorid, k1.$kandidatlisteid FROM $kandidatutfallTabell k1,
                        (SELECT MAX($tidspunkt) as maksTidspunkt FROM $kandidatutfallTabell k2
                            WHERE k2.$tidspunkt BETWEEN ? AND ?
                            GROUP BY $aktorid, $kandidatlisteid
                        ) as k2
                     WHERE k1.$navkontor = ? 
                      AND k1.$tidspunkt = k2.maksTidspunkt
                      AND (k1.$utfall = '${FATT_JOBBEN.name}' OR k1.$utfall = '${PRESENTERT.name}')
                ) as unike_presenteringer_per_person_og_liste
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

    fun hentAntallPresentertForAlleNavKontor(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_presenteringer_per_person_og_liste.*) FROM (
                    SELECT DISTINCT $aktorid, $kandidatlisteid FROM $kandidatutfallTabell
                      WHERE ($utfall = '${FATT_JOBBEN.name}' OR $utfall = '${PRESENTERT.name}')
                ) as unike_presenteringer_per_person_og_liste
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater fra databasen")
            }
        }
    }

    fun hentAntallFåttJobbenForAlleNavKontor(): Int {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT COUNT(unike_presenteringer_per_person_og_liste.*) FROM (
                    SELECT DISTINCT $aktorid, $kandidatlisteid FROM $kandidatutfallTabell
                      WHERE ($utfall = '${FATT_JOBBEN.name}')
                ) as unike_presenteringer_per_person_og_liste
            """.trimIndent()
            ).executeQuery()

            if (resultSet.next()) {
                return resultSet.getInt(1)
            } else {
                throw RuntimeException("Prøvde å hente antall presenterte kandidater fra databasen")
            }
        }
    }

    fun hentAktoriderForFåttJobben(hentStatistikk: HentStatistikk): List<String> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT k1.* FROM $kandidatutfallTabell k1,
                
                  (SELECT MAX($tidspunkt) as maksTidspunkt FROM $kandidatutfallTabell k2
                     WHERE k2.$tidspunkt BETWEEN ? AND ?
                     GROUP BY $aktorid, $kandidatlisteid) as k2
                     
                WHERE k1.$navkontor = ?
                  AND k1.$tidspunkt = k2.maksTidspunkt
                  AND k1.$utfall = '${FATT_JOBBEN.name}'
            """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(hentStatistikk.fraOgMed))
                setDate(2, Date.valueOf(hentStatistikk.tilOgMed))
                setString(3, hentStatistikk.navKontor)
            }.executeQuery()

            return generateSequence {
                if (!resultSet.next()) null
                else resultSet.getString(aktorid)
            }.toList()

        }
    }

    class UtfallElement(
        val harHull: Boolean?,
        val alder: Int?,
        val tidspunkt: LocalDateTime,
        val tilretteleggingsbehov: List<String>,
        val synligKandidat: Boolean
    )

    private fun ResultSet.toUtfallElement() = UtfallElement(
        harHull = if (getObject(1) == null) null else getBoolean(1),
        alder = if (getObject(2) == null) null else getInt(2),
        tidspunkt = getTimestamp(3).toLocalDateTime(),
        tilretteleggingsbehov = if (getObject(4) == null || getString(4).isBlank()) emptyList() else getString(4).split(
            tilretteleggingsbehovdelimiter
        ),
        synligKandidat = if (getObject(5) == null) false else getBoolean(5)
    )

    fun hentUtfallPresentert(fraOgMed: LocalDate): List<UtfallElement> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                    with KANDIDATER_SOM_FIKK_JOBBEN_UTEN_AA_HA_BLITT_PRESENTERT_FØRST as (
                        SELECT $hullICv, $alder, $tidspunkt, $tilretteleggingsbehov, $synligKandidat FROM $kandidatutfallTabell k1,
                            (SELECT MIN($dbId) as $dbId from $kandidatutfallTabell k2
                            WHERE k2.$tidspunkt >= ?
                            GROUP BY $aktorid, $kandidatlisteid) as k2

                        WHERE k1.$dbId = k2.$dbId
                        AND k1.$utfall = '${FATT_JOBBEN.name}'
                    ),
                    PRESENTERTE_KANDIDATER as (
                        SELECT $hullICv, $alder, $tidspunkt, $tilretteleggingsbehov, $synligKandidat from $kandidatutfallTabell k1,
                            (
                                SELECT MAX($dbId) as maksId from $kandidatutfallTabell k2
                                WHERE k2.$utfall = '${PRESENTERT}'
                                GROUP BY $aktorid, $kandidatlisteid
                            ) k2
                        WHERE $tidspunkt >= ?
                        AND $dbId = k2.maksId
                    )
                    SELECT $hullICv, $alder, $tidspunkt, $tilretteleggingsbehov, $synligKandidat from KANDIDATER_SOM_FIKK_JOBBEN_UTEN_AA_HA_BLITT_PRESENTERT_FØRST
                    UNION ALL
                    SELECT $hullICv, $alder, $tidspunkt, $tilretteleggingsbehov, $synligKandidat from PRESENTERTE_KANDIDATER;
                """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
                setDate(2, Date.valueOf(fraOgMed))
            }.executeQuery()
            val utfallElementer = mutableListOf<UtfallElement>()

            while (resultSet.next()) {
                utfallElementer += resultSet.toUtfallElement()
            }
            return utfallElementer
        }
    }

    fun hentUtfallFåttJobben(fraOgMed: LocalDate): List<UtfallElement> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement(
                """
                SELECT telleliste.$hullICv, telleliste.$alder, tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon.$tidspunkt, telleliste.$tilretteleggingsbehov, telleliste.$synligKandidat FROM $kandidatutfallTabell telleliste,
                  (SELECT MIN($dbId) as minId, tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$tidspunkt FROM $kandidatutfallTabell tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon,
                    (SELECT MAX($dbId) as maksId, senesteUtfallITidsromOgFåttJobben.$tidspunkt FROM $kandidatutfallTabell tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon,
                      (SELECT senesteUtfallITidsromOgFåttJobben.$aktorid, senesteUtfallITidsromOgFåttJobben.$kandidatlisteid, senesteUtfallITidsromOgFåttJobben.$tidspunkt FROM $kandidatutfallTabell senesteUtfallITidsromOgFåttJobben,  
                          (SELECT MAX($dbId) as maksId FROM $kandidatutfallTabell senesteUtfallITidsrom
                          WHERE senesteUtfallITidsrom.$tidspunkt >= ?
                          GROUP BY senesteUtfallITidsrom.$aktorid, senesteUtfallITidsrom.$kandidatlisteid) as senesteUtfallITidsrom
                      WHERE senesteUtfallITidsromOgFåttJobben.$dbId = senesteUtfallITidsrom.maksId
                      AND senesteUtfallITidsromOgFåttJobben.$utfall = '${FATT_JOBBEN.name}') as senesteUtfallITidsromOgFåttJobben                  
                    WHERE tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$aktorid = senesteUtfallITidsromOgFåttJobben.$aktorid
                    AND tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$kandidatlisteid = senesteUtfallITidsromOgFåttJobben.$kandidatlisteid
                    GROUP BY tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$aktorid, tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$kandidatlisteid, tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$utfall, senesteUtfallITidsromOgFåttJobben.$tidspunkt) as tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon
                  WHERE tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon.$dbId = tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.maksId
                  GROUP BY tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon.$aktorid, tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon.$kandidatlisteid, tidligsteUtfallPaaAktorIdKandidatlisteKombinasjon.$tidspunkt) as tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon
                WHERE  telleliste.$dbId = tidligsteUtfallPaaAktorIdKandidatlisteUtfallKombinasjon.minId
            """
            ).apply {
                setDate(1, Date.valueOf(fraOgMed))
            }.executeQuery()
            val utfallElementer = mutableListOf<UtfallElement>()

            while (resultSet.next()) {
                utfallElementer += resultSet.toUtfallElement()
            }
            return utfallElementer
        }
    }

    companion object {
        const val dbId = "id"
        const val kandidatutfallTabell = "kandidatutfall"
        const val aktorid = "aktorid"
        const val fnr = "fnr"
        const val utfall = "utfall"
        const val navident = "navident"
        const val navkontor = "navkontor"
        const val kandidatlisteid = "kandidatlisteid"
        const val stillingsid = "stillingsid"
        const val synligKandidat = "synlig_kandidat"
        const val tidspunkt = "tidspunkt"
        const val hullICv = "hull_i_cv"
        const val innsatsbehov = "innsatsbehov"
        const val hovedmål = "hovedmaal"
        const val sendtStatus = "sendt_status"
        const val antallSendtForsøk = "antall_sendt_forsok"
        const val sisteSendtForsøk = "siste_sendt_forsok"
        const val alder = "alder"
        const val tilretteleggingsbehov = "tilretteleggingsbehov"
        const val tilretteleggingsbehovdelimiter = ";"


        fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
            Kandidatutfall(
                dbId = resultSet.getLong(dbId),
                aktorId = resultSet.getString(aktorid),
                utfall = Utfall.valueOf(resultSet.getString(utfall)),
                navIdent = resultSet.getString(navident),
                navKontor = resultSet.getString(navkontor),
                kandidatlisteId = UUID.fromString(resultSet.getString(kandidatlisteid)),
                stillingsId = UUID.fromString(resultSet.getString(stillingsid)),
                synligKandidat = if (resultSet.getObject(synligKandidat) == null) null else resultSet.getBoolean(
                    synligKandidat
                ),
                hullICv = if (resultSet.getObject(hullICv) == null) null else resultSet.getBoolean(hullICv),
                innsatsbehov = resultSet.getString(innsatsbehov),
                hovedmål = resultSet.getString(hovedmål),
                tidspunkt = resultSet.getTimestamp(tidspunkt).toLocalDateTime(),
                antallSendtForsøk = resultSet.getInt(antallSendtForsøk),
                sendtStatus = SendtStatus.valueOf(resultSet.getString(sendtStatus)),
                sisteSendtForsøk = resultSet.getTimestamp(sisteSendtForsøk)?.toLocalDateTime(),
                alder = if (resultSet.getObject(alder) == null) null else resultSet.getInt(alder),
                tilretteleggingsbehov = if (resultSet.getString(tilretteleggingsbehov)
                        .isBlank()
                ) emptyList() else resultSet.getString(tilretteleggingsbehov).split(tilretteleggingsbehovdelimiter)
            )
    }
}
