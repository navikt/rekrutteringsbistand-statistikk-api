package no.nav.statistikkapi.db

import no.nav.statistikkapi.atOslo
import no.nav.statistikkapi.kandidatliste.Kandidatliste
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository
import no.nav.statistikkapi.kandidatliste.KandidatlisteRepository.Companion.konverterTilKandidatliste
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository.Companion.konverterTilKandidatutfall
import no.nav.statistikkapi.stillinger.Stilling
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.konverterTilStilling
import no.nav.statistikkapi.tiltak.TiltaksRepository
import java.time.ZonedDateTime
import javax.sql.DataSource

class TestRepository(private val dataSource: DataSource) {

    fun slettAlleUtfall() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${KandidatutfallRepository.kandidatutfallTabell}").execute()
        }
    }

    fun slettAlleLønnstilskudd() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM ${TiltaksRepository.tiltaksTabellLabel}").execute()
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

    fun hentTiltak() = dataSource.connection.use {
        it.prepareStatement("SELECT ${TiltaksRepository.sistEndretLabel}, ${TiltaksRepository.tiltakstypeLabel}, ${TiltaksRepository.avtaleInngåttLabel}" +
                " FROM ${TiltaksRepository.tiltaksTabellLabel}").executeQuery().run {
            next()
            TiltakRad(
                getTimestamp(TiltaksRepository.sistEndretLabel).toInstant().atOslo(),
                getTimestamp(TiltaksRepository.avtaleInngåttLabel).toInstant().atOslo(),
                getString(TiltaksRepository.tiltakstypeLabel))
        }
    }
    class TiltakRad(val sistEndret: ZonedDateTime, val avtaleInngått: ZonedDateTime, val tiltakstype: String)

}
