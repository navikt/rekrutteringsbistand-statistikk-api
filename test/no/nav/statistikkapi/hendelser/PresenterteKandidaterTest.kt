package no.nav.statistikkapi.hendelser

import assertk.assertThat
import assertk.assertions.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class RegistrertDeltCvTest {

    companion object {
        private val database = TestDatabase()
        private val rapid: TestRapid = TestRapid()
        private val testRepository = TestRepository(database.dataSource)

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            start(database = database, rapid = rapid, port = randomPort())
        }
    }

    @After
    fun afterEach() {
        testRepository.slettAlleUtfall()
        testRepository.slettAlleStillinger()
        rapid.reset()
    }

    @Test
    fun `mottak av kandidatutfall skal være idempotent`() {
        rapid.sendTestMessage(melding)
        rapid.sendTestMessage(melding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
    }

    @Test
    fun `Kan opprette kandidatutfall av RegistrertDeltCv melding`() {
        rapid.sendTestMessage(melding)

        val utfall = testRepository.hentUtfall()
        assertThat(utfall).size().isEqualTo(2)
        utfall[0].apply {
            assertThat(stillingsId).isEqualTo(UUID.fromString("b5919e46-9882-4b3c-8089-53ad02f26023"))
            assertThat(kandidatlisteId).isEqualTo(UUID.fromString("d5b5b4c1-0375-4719-9038-ab31fe27fb40"))
            assertThat(navIdent).isEqualTo("Z994633")
            assertThat(navKontor).isEqualTo("0313")
            assertThat(tidspunkt).isEqualTo(LocalDateTime.of(2023, 2, 9, 9, 45, 53, 649_000_000))
            assertThat(utfall).isEqualTo(Utfall.PRESENTERT)
            assertThat(synligKandidat!!).isTrue()

            assertThat(aktorId).isEqualTo("2452127907551")
            assertThat(alder).isEqualTo(51)
            assertThat(tilretteleggingsbehov).isEmpty()
            assertThat(hullICv!!).isFalse()
            assertThat(innsatsbehov).isEqualTo("BFORM")
            assertThat(hovedmål).isEqualTo("BEHOLDEA")


        }
    }
}

private val melding = """
        {
          TODO: Legg til medling of tilpass assertionene over
        }
    """.trimIndent()
