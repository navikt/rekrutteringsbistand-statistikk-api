package no.nav.statistikkapi.visningkontaktinfo

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.etKandidatutfall
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall
import no.nav.statistikkapi.nowOslo
import org.junit.After
import org.junit.Test
import java.util.*
import kotlin.test.assertNotNull


class VisningKontaktinfoRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val visningKontaktinfoRepo = VisningKontaktinfoRepository(database.dataSource)
        private val kandidatutfallRepository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)
    }

    @After
    fun afterEach() {
        testRepository.slettAlleUtfall()
        testRepository.slettAlleVisningKontaktinfo()
    }

    @Test
    fun `Skal kunne lagre en visningAvKontaktinfo`() {
        val aktørId = "1010101010"
        val stillingsId = UUID.randomUUID()
        val tidspunkt = nowOslo()

        visningKontaktinfoRepo.lagre(aktørId, stillingsId, tidspunkt)

        val lagreteVisningKontaktinfo = testRepository.hentVisningKontaktinfo()
        assertThat(lagreteVisningKontaktinfo.size).isEqualTo(1)
        val lagretVisning = lagreteVisningKontaktinfo.first()
        assertNotNull(lagretVisning.dbId)
        assertThat(lagretVisning.aktørId).isEqualTo(aktørId)
        assertThat(lagretVisning.stillingId).isEqualTo(stillingsId)
        assertThat(lagretVisning.tidspunkt).isEqualTo(tidspunkt)
    }

    @Test
    fun `Skal kunne telle antall kandidater i prioritert målgruppe som har fått åpnet sin kontaktinformasjon`() {
        val kandidatUtfallIPrioritertMålgruppe =
            etKandidatutfall.copy(aktørId = "1", utfall = Utfall.PRESENTERT, harHullICv = true)
        kandidatutfallRepository.lagreUtfall(kandidatUtfallIPrioritertMålgruppe)
        visningKontaktinfoRepo.lagre(
            kandidatUtfallIPrioritertMålgruppe.aktørId,
            UUID.fromString(kandidatUtfallIPrioritertMålgruppe.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal ikke telle kandidat som ikke er i prioritert målgruppe selv om vedkommende fikk åpnet sin kontaktinformasjon`() {
        val kandidatUtfallIkkeIPrioritertMålgruppe = etKandidatutfall.copy(
            aktørId = "2",
            utfall = Utfall.PRESENTERT,
            harHullICv = false,
            alder = 33,
            innsatsbehov = "IKVAL"
        )
        kandidatutfallRepository.lagreUtfall(kandidatUtfallIkkeIPrioritertMålgruppe)
        visningKontaktinfoRepo.lagre(
            kandidatUtfallIkkeIPrioritertMålgruppe.aktørId,
            UUID.fromString(kandidatUtfallIkkeIPrioritertMålgruppe.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal ikke telle kandidat i prioritert målgruppe som ikke fikk åpnet sin kontaktinformasjon`() {

    }

    @Test
    fun `Skal kun telle én gang per kandidat selv om vedkommende har fått åpnet sin kontaktinfo flere ganger på en stilling`() {}

    @Test
    fun `Skal telle flere ganger per kandidat når vedkommende har fått åpnet sin kontaktinfo i flere kandidatlister`() {}

    @Test
    fun `Kandidat under 30 år skal telles i prioritert målgruppe`() {
    }

    @Test
    fun `Kandidat over 50 år skal telles i prioritert målgruppe`() {
    }

    @Test
    fun `Kandidat med innsatsgruppe situasjonsbestemt innsats skal telles i prioritert målgruppe`() {
    }

    @Test
    fun `Kandidat med innsatsgruppe spesielt tilpasset innsats skal telles i prioritert målgruppe`() {
    }

    @Test
    fun `Kandidat med innsatsgruppe varig tilpasset innsats skal telles i prioritert målgruppe`() {
    }
}