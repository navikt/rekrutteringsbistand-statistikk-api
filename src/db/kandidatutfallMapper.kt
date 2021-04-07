package no.nav.rekrutteringsbistand.statistikk.db

import java.sql.ResultSet
import java.util.*

fun konverterTilKandidatutfall(resultSet: ResultSet): Kandidatutfall =
    Kandidatutfall(
        dbId = resultSet.getLong(Repository.dbId),
        aktorId = resultSet.getString(Repository.aktørId),
        utfall = Utfall.valueOf(resultSet.getString(Repository.utfall)),
        navIdent = resultSet.getString(Repository.navident),
        navKontor = resultSet.getString(Repository.navkontor),
        kandidatlisteId = UUID.fromString(resultSet.getString(Repository.kandidatlisteid)),
        stillingsId = UUID.fromString(resultSet.getString(Repository.stillingsid)),
        hullICv = resultSet.getBoolean(Repository.hullICv),
        tidspunkt = resultSet.getTimestamp(Repository.tidspunkt).toLocalDateTime(),
        antallSendtForsøk = resultSet.getInt(Repository.antallSendtForsøk),
        sendtStatus = SendtStatus.valueOf(resultSet.getString(Repository.sendtStatus)),
        sisteSendtForsøk = resultSet.getTimestamp(Repository.sisteSendtForsøk)?.toLocalDateTime()
    )
