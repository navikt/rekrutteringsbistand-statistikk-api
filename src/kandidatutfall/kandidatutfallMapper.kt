package no.nav.rekrutteringsbistand.statistikk.kandidatutfall

import java.sql.ResultSet
import java.util.*

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
