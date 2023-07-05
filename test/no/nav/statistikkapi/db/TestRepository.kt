package no.nav.statistikkapi.db

import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository.Companion.konverterTilKandidatutfall
import no.nav.statistikkapi.stillinger.Stilling
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingRepository.Companion.konverterTilStilling
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class TestRepository(private val dataSource: DataSource) {

    fun slettAlleUtfall() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${KandidatutfallRepository.kandidatutfallTabell}").execute()
        }
    }

    fun hentUtfall(): List<Kandidatutfall> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM ${KandidatutfallRepository.kandidatutfallTabell} ORDER BY id ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatutfall(resultSet)
                else null
            }.toList()
        }
    }

    fun hentKandidatlister(): List<Kandidatliste> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM ${KandidatlisteRepository.kandidatlisteTabell} ORDER BY id ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilKandidatliste(resultSet)
                else null
            }.toList()
        }
    }

    fun konverterTilKandidatliste(resultSet: ResultSet): Kandidatliste =
        Kandidatliste(
            dbId = resultSet.getLong(KandidatlisteRepository.dbIdKolonne),
            kandidatlisteId = UUID.fromString(resultSet.getString(KandidatlisteRepository.kandidatlisteIdKolonne)),
            stillingsId = UUID.fromString(resultSet.getString(KandidatlisteRepository.stillingsIdKolonne)),
            erDirektemeldt = resultSet.getBoolean(KandidatlisteRepository.erDirektemeldtKolonne),
            antallStillinger = resultSet.getInt(KandidatlisteRepository.antallStillingerKolonne),
            antallKandidater = resultSet.getInt(KandidatlisteRepository.antallKandidaterKolonne),
            stillingOpprettetTidspunkt = resultSet.getTimestamp(KandidatlisteRepository.stillingOpprettetTidspunktKolonne)?.toInstant()?.atOslo(),
            stillingensPubliseringstidspunkt = resultSet.getTimestamp(KandidatlisteRepository.stillingensPubliseringstidspunktKolonne)
                .toInstant().atOslo(),
            organisasjonsnummer = resultSet.getString(KandidatlisteRepository.organisasjonsnummerKolonne),
            utførtAvNavIdent = resultSet.getString(KandidatlisteRepository.utførtAvNavIdentKolonne),
            tidspunkt = resultSet.getTimestamp(KandidatlisteRepository.tidspunktForHendelsenKolonne).toInstant().atOslo(),
            eventName = resultSet.getString(KandidatlisteRepository.eventNameKolonne)
        )

    fun slettAlleKandidatlister() {
        dataSource.connection.use {
            it.prepareStatement("delete from ${KandidatlisteRepository.kandidatlisteTabell}").execute()
        }
    }

    fun hentStilling(): List<Stilling> {
        dataSource.connection.use {
            val resultSet =
                it.prepareStatement("SELECT * FROM ${StillingRepository.stillingtabell} ORDER BY UUID ASC")
                    .executeQuery()
            return generateSequence {
                if (resultSet.next()) konverterTilStilling(resultSet)
                else null
            }.toList()
        }
    }

    fun hentAntallStillinger() = dataSource.connection.use {
        it.prepareStatement("SELECT count(*) FROM ${StillingRepository.stillingtabell}").executeQuery().run {
            next()
            getInt(1)
        }
    }

    fun slettAlleStillinger() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${StillingRepository.stillingtabell}").execute()
        }
    }

    fun hentVisningKontaktinfo(): List<VisningKontaktinfoMedDbId> = dataSource.connection.use {
        val resultSet =
            it.prepareStatement("select * from visning_kontaktinfo")
                .executeQuery()
        return generateSequence {
            if (resultSet.next()) {
                VisningKontaktinfoMedDbId(
                    dbId = resultSet.getLong("id"),
                    aktørId = resultSet.getString("aktør_id"),
                    stillingId = UUID.fromString(resultSet.getString("stilling_id")),
                    tidspunkt = resultSet.getTimestamp("tidspunkt").toInstant().atOslo()
                )
            }
            else null
        }.toList()
    }

    fun slettAlleVisningKontaktinfo() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM visning_kontaktinfo").execute()
        }
    }

    data class VisningKontaktinfoMedDbId(
        val dbId: Long,
        val aktørId: String,
        val stillingId: UUID,
        val tidspunkt: ZonedDateTime
    )

    data class Kandidatliste(
        val dbId: Long,
        val kandidatlisteId: UUID,
        val stillingsId: UUID,
        val erDirektemeldt: Boolean,
        val stillingOpprettetTidspunkt: ZonedDateTime?,
        val stillingensPubliseringstidspunkt: ZonedDateTime,
        val organisasjonsnummer: String,
        val antallStillinger: Int,
        val antallKandidater: Int,
        val tidspunkt: ZonedDateTime,
        val utførtAvNavIdent: String,
        val eventName: String
    )
}
