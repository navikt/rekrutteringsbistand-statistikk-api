package no.nav.statistikkapi.visningkontaktinfo

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.nowOslo
import org.junit.After
import org.junit.Test
import java.util.*
import kotlin.test.assertNotNull


class VisningKontaktinfoRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val visningKontaktinfoRepository = VisningKontaktinfoRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
    }

    @After
    fun afterEach() {
        testRepository.slettAlleKandidatlister()
    }

    @Test
    fun `Skal kunne lagre en visningAvKontaktinfo`() {
        val visningAvKontaktinfo = visningAvKontaktinfo()

        visningKontaktinfoRepository.lagre(visningAvKontaktinfo)

        val lagreteVisningKontaktinfo = testRepository.hentVisningKontaktinfo()
        assertThat(lagreteVisningKontaktinfo.size).isEqualTo(1)
        val lagretVisning = lagreteVisningKontaktinfo.first()
        assertNotNull(lagretVisning.dbId)
        assertThat(lagretVisning.aktørId).isEqualTo(visningAvKontaktinfo.aktørId)
        assertThat(lagretVisning.stillingId).isEqualTo(visningAvKontaktinfo.stillingId)
        assertThat(lagretVisning.tidspunkt).isEqualTo(visningAvKontaktinfo.tidspunkt)
    }

    private fun visningAvKontaktinfo() = VisningKontaktinfo(
        aktørId = "1010101010",
        stillingId = UUID.randomUUID(),
        tidspunkt = nowOslo()
    )
}