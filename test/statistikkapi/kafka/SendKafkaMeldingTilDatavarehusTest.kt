package statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import no.finn.unleash.FakeUnleash
import org.junit.After
import org.junit.Test
import statistikkapi.db.TestDatabase
import statistikkapi.db.TestRepository
import statistikkapi.etKandidatutfall
import statistikkapi.kandidatutfall.Kandidatutfall
import statistikkapi.kandidatutfall.KandidatutfallRepository
import statistikkapi.kandidatutfall.SendtStatus.IKKE_SENDT
import statistikkapi.kandidatutfall.SendtStatus.SENDT
import java.time.LocalDateTime.now

class SendKafkaMeldingTilDatavarehusTest {

    companion object {
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
        private val unleash = FakeUnleash().apply {
            enableAll()
        }

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
        repository.lagreUtfall(etKandidatutfall, now())
        repository.lagreUtfall(etKandidatutfall.copy(aktørId = "10000254879659"), now())
        hentUsendteUtfallOgSendPåKafka(repository, producerSomFeilerEtterFørsteKall, unleash).run()

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

    @Test
    fun `Skal ikke sende kandidatutfall til Kafka hvis feature toggle er slått av`() {
        repository.lagreUtfall(etKandidatutfall, now())
        val unleashMedSlåttAvFeatureToggle = FakeUnleash()
        hentUsendteUtfallOgSendPåKafka(repository, producerSomFeilerEtterFørsteKall, unleashMedSlåttAvFeatureToggle).run()

        val lagraUtfall = testRepository.hentUtfall()[0]
        assertThat(lagraUtfall.sendtStatus).isEqualTo(IKKE_SENDT)
        assertThat(lagraUtfall.antallSendtForsøk).isEqualTo(0)
        assertThat(lagraUtfall.sisteSendtForsøk).isNull()
    }

    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
    }
}
