package kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import db.TestDatabaseImpl
import db.TestRepository
import etKandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Kandidatutfall
import no.nav.rekrutteringsbistand.statistikk.db.Repository
import no.nav.rekrutteringsbistand.statistikk.db.SendtStatus.IKKE_SENDT
import no.nav.rekrutteringsbistand.statistikk.kafka.DatavarehusKafkaProducer
import no.nav.rekrutteringsbistand.statistikk.kafka.sendKafkaMeldingTilDatavarehus
import org.junit.After
import org.junit.Test
import java.lang.Exception
import java.time.LocalDateTime.now

class SendKafkaMeldingTilDatavarehusTest {

    companion object {
        private val database = TestDatabaseImpl()
        private val repository = Repository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)

        private val producerSomFeiler = object : DatavarehusKafkaProducer {
            override fun send(kandidatutfall: Kandidatutfall) {
                throw Exception()
            }
        }
    }

    @Test
    fun `Feilsending med Kafka skal oppdatere antallSendtForsøk og sisteSendtForsøk`() {
        repository.lagreUtfall(etKandidatutfall)
        sendKafkaMeldingTilDatavarehus(repository, producerSomFeiler).run()

        val nå = now()
        val utfall = testRepository.hentUtfall()[0]
        assertThat(utfall.sendtStatus).isEqualTo(IKKE_SENDT)
        assertThat(utfall.antallSendtForsøk).isEqualTo(1)
        assertThat(utfall.sisteSendtForsøk!!).isBetween(nå.minusSeconds(10), nå)

        sendKafkaMeldingTilDatavarehus(repository, producerSomFeiler).run()
        val utfall2 = testRepository.hentUtfall()[0]
        assertThat(utfall2.antallSendtForsøk).isEqualTo(2)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
