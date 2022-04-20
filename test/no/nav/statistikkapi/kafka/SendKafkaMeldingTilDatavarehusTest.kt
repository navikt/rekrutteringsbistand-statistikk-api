package no.nav.statistikkapi.kafka

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isZero
import io.mockk.every
import io.mockk.mockk
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.enElasticSearchStilling
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.kandidatutfall.Kandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.SendtStatus.IKKE_SENDT
import no.nav.statistikkapi.kandidatutfall.SendtStatus.SENDT
import no.nav.statistikkapi.stillinger.ElasticSearchKlient
import no.nav.statistikkapi.stillinger.StillingRepository
import no.nav.statistikkapi.stillinger.StillingService
import no.nav.statistikkapi.stillinger.Stillingskategori
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime.now

class SendKafkaMeldingTilDatavarehusTest {

    companion object {
        private val database = TestDatabase()
        private val utfallRepo = KandidatutfallRepository(database.dataSource)
        private val stillingRepo = StillingRepository(database.dataSource)
        private val elasticSearchKlientMock = mockk<ElasticSearchKlient>()
        private val stillingService = StillingService(elasticSearchKlientMock, stillingRepo)
        private val testRepository = TestRepository(database.dataSource)

        private val producerSomFeilerEtterFørsteKall = object : DatavarehusKafkaProducer {
            var førsteKall = true
            override fun send(kandidatutfall: Kandidatutfall, stillingskategori: Stillingskategori) {
                if (førsteKall) {
                    førsteKall = false
                    return
                } else throw Exception()
            }
        }

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testRepository.slettAlleUtfall()
            testRepository.slettAlleStillinger()
        }
    }

    @Test
    fun `Feilsending med Kafka skal oppdatere antallSendtForsøk og sisteSendtForsøk`() {
        every { elasticSearchKlientMock.hentStilling(etKandidatutfall.stillingsId) } returns enElasticSearchStilling()
        assertThat(etKandidatutfall.stillingsId).isEqualTo(enElasticSearchStilling().uuid)
        utfallRepo.lagreUtfall(etKandidatutfall, now())
        utfallRepo.lagreUtfall(etKandidatutfall.copy(aktørId = "10000254879659"), now())
        assertThat(testRepository.hentAntallStillinger()).isZero()

        hentUsendteUtfallOgSendPåKafka(utfallRepo, producerSomFeilerEtterFørsteKall, stillingService).run()

        val nå = now()
        assertThat(testRepository.hentAntallStillinger()).isEqualTo(1)
        val vellyketUtfall = testRepository.hentUtfall()[0]
        assertThat(stillingRepo.hentNyesteStilling(vellyketUtfall.stillingsId)).isNotNull()
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
        testRepository.slettAlleStillinger()
    }
}
