package kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import db.TestDatabase
import db.TestRepository
import etKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.IKKE_SENDT
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.SENDT
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.kafka.hentUsendteUtfallOgSendPåKafka
import org.junit.After
import org.junit.Test
import java.time.LocalDateTime.now

class SendKafkaMeldingTilDatavarehusTest {

    companion object {
        private val database = TestDatabase()
        private val repository = Repository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)

        private val producerSomFeilerEtterFørsteKall = object : DatavarehusKafkaProducer {
            var førsteKall = true
            override fun send(kandidatutfall: Kandidatutfall) {
                if (førsteKall) {
                    førsteKall = false
                    return
                } else throw Exception()
            }
        }
    }

    @Test
    fun `Feilsending med Kafka skal oppdatere antallSendtForsøk og sisteSendtForsøk`() {
        repository.lagreUtfall(etKandidatutfall)
        repository.lagreUtfall(etKandidatutfall.copy(aktørId = "10000254879659"))
        hentUsendteUtfallOgSendPåKafka(repository, producerSomFeilerEtterFørsteKall).run()

        val nå = now()
        val vellyketUtfall = testRepository.hentUtfall()[0]
        assertThat(vellyketUtfall.sendtStatus).isEqualTo(SENDT)
        assertThat(vellyketUtfall.antallSendtForsøk).isEqualTo(1)
        assertThat(vellyketUtfall.sisteSendtForsøk!!).isBetween(nå.minusSeconds(10), nå)

        val feiletUtfall = testRepository.hentUtfall()[1]
        assertThat(feiletUtfall.sendtStatus).isEqualTo(IKKE_SENDT)
        assertThat(feiletUtfall.antallSendtForsøk).isEqualTo(1)
        assertThat(feiletUtfall.sisteSendtForsøk!!).isBetween(nå.minusSeconds(10), nå)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
