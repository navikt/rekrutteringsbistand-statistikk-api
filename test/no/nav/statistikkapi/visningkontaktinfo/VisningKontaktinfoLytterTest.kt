package no.nav.statistikkapi.visningkontaktinfo

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.nowOslo
import no.nav.statistikkapi.randomPort
import no.nav.statistikkapi.start
import org.junit.After
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*

class VisningKontaktinfoLytterTest {

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
        testRepository.slettAlleKandidatlister()
        rapid.reset()
    }

    @Ignore
    @Test
    fun `Skal lagre visning av kontaktinfo når vi mottar melding`() {
        val stillingsId = UUID.randomUUID()
        val tidspunkt = nowOslo()
        val melding = visningKontaktinfoMelding(
            aktørId = "10108000398",
            stillingsId = stillingsId,
            tidspunkt = tidspunkt
        )

        rapid.sendTestMessage(melding)

        val lagretVisningerAvKontaktinfo = testRepository.hentVisningKontaktinfo()

        assertThat(lagretVisningerAvKontaktinfo.size).isEqualTo(1)

        val lagretVisning = lagretVisningerAvKontaktinfo.first()
        assertThat(lagretVisning.aktørId).isEqualTo("10108000398")
        assertThat(lagretVisning.stillingId).isEqualTo(stillingsId)
        assertThat(lagretVisning.tidspunkt).isEqualTo(tidspunkt)
    }

    @Ignore
    @Test
    fun `Behandling av melding skal være idempotent`() {
        val stillingsId = UUID.randomUUID()
        val tidspunkt = nowOslo()
        val melding = visningKontaktinfoMelding(
            aktørId = "10108000398",
            stillingsId = stillingsId,
            tidspunkt = tidspunkt
        )

        rapid.sendTestMessage(melding)
        rapid.sendTestMessage(melding)

        val lagretVisningerAvKontaktinfo = testRepository.hentVisningKontaktinfo()

        assertThat(lagretVisningerAvKontaktinfo.size).isEqualTo(1)

    }

    private fun visningKontaktinfoMelding(
        aktørId: String,
        stillingsId: UUID,
        tidspunkt: ZonedDateTime
    ) =
        """
             "@event_name": "arbeidsgiversKandidatliste.VisningKontaktinfo",
             "aktørId": "$aktørId",
             "stillingsId": "$stillingsId",
             "tidspunkt": "$tidspunkt"
        """.trimIndent()
}