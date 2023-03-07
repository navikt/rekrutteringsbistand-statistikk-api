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
        val kandidatutfall = kandidatutfallIPrioritertMålgruppe
        kandidatutfallRepository.lagreUtfall(kandidatutfall)
        visningKontaktinfoRepo.lagre(
            kandidatutfall.aktørId,
            UUID.fromString(kandidatutfall.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal ikke telle kandidat som ikke er i prioritert målgruppe selv om vedkommende fikk åpnet sin kontaktinformasjon`() {
        val kandidatutfall = kandidatutfallIkkeIPrioritertMålgruppe
        kandidatutfallRepository.lagreUtfall(kandidatutfallIkkeIPrioritertMålgruppe)
        visningKontaktinfoRepo.lagre(
            kandidatutfall.aktørId,
            UUID.fromString(kandidatutfall.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal ikke telle kandidat i prioritert målgruppe som ikke fikk åpnet sin kontaktinformasjon`() {
        kandidatutfallRepository.lagreUtfall(kandidatutfallIPrioritertMålgruppe)

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal kun telle én gang per kandidat selv om vedkommende har fått åpnet sin kontaktinfo flere ganger på en stilling`() {
        val kandidatutfall = kandidatutfallIPrioritertMålgruppe
        kandidatutfallRepository.lagreUtfall(kandidatutfall)
        visningKontaktinfoRepo.lagre(
            kandidatutfall.aktørId,
            UUID.fromString(kandidatutfall.stillingsId),
            tidspunkt = nowOslo().minusHours(2)
        )
        visningKontaktinfoRepo.lagre(
            kandidatutfall.aktørId,
            UUID.fromString(kandidatutfall.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal telle flere ganger per kandidat når vedkommende har fått åpnet sin kontaktinfo i flere kandidatlister`() {
        val kandidatutfallIFørsteListe = kandidatutfallIPrioritertMålgruppe
        val kandiatutfallIAndreListe = kandidatutfallIPrioritertMålgruppe.copy(
            aktørId = kandidatutfallIFørsteListe.aktørId,
            stillingsId = UUID.randomUUID().toString(),
            kandidatlisteId = "annenKandidatlisteId"
        )
        kandidatutfallRepository.lagreUtfall(kandidatutfallIFørsteListe)
        kandidatutfallRepository.lagreUtfall(kandiatutfallIAndreListe)
        visningKontaktinfoRepo.lagre(
            kandidatutfallIFørsteListe.aktørId,
            UUID.fromString(kandidatutfallIFørsteListe.stillingsId),
            tidspunkt = nowOslo()
        )
        visningKontaktinfoRepo.lagre(
            kandiatutfallIAndreListe.aktørId,
            UUID.fromString(kandiatutfallIAndreListe.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(2)
    }

    @Test
    fun `Kandidat under 30 år skal telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(alder = 29)
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
    fun `Kandidat over 50 år skal telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(alder = 51)
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
    fun `Kandidat med innsatsgruppe situasjonsbestemt innsats skal telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(innsatsbehov = "BFORM")
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
    fun `Kandidat med innsatsgruppe spesielt tilpasset innsats skal telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(innsatsbehov = "BATT")
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
    fun `Kandidat med innsatsgruppe varig tilpasset innsats skal telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(innsatsbehov = "VARIG")
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
    fun `Kandidat med innsatsgruppe standardinnsats skal ikke telles i prioritert målgruppe`() {
        val kandidatUtfallIPrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe.copy(innsatsbehov = "IKVAL")
        kandidatutfallRepository.lagreUtfall(kandidatUtfallIPrioritertMålgruppe)
        visningKontaktinfoRepo.lagre(
            kandidatUtfallIPrioritertMålgruppe.aktørId,
            UUID.fromString(kandidatUtfallIPrioritertMålgruppe.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal kunne telle mange kandidater med åpnet kontaktinfo`() {

    }

    private val kandidatutfallIPrioritertMålgruppe = etKandidatutfall.copy(aktørId = "1", utfall = Utfall.PRESENTERT, harHullICv = true)
    private val kandidatutfallIkkeIPrioritertMålgruppe = etKandidatutfall.copy(
        aktørId = "2",
        utfall = Utfall.PRESENTERT,
        harHullICv = false,
        alder = 33,
        innsatsbehov = "IKVAL"
    )
}