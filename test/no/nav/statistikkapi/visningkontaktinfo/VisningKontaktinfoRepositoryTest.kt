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
    fun `skal teste noe`() {
        val kandidatutfall1 = kandidatutfallIPrioritertMålgruppe.copy(aktørId = "10", stillingsId = "8be310b1-bee7-4aa2-80a6-146c247dca45")
        val kandidatutfall2 = kandidatutfallIPrioritertMålgruppe.copy(aktørId = "10", stillingsId = "3ebb877f-a010-4b1a-ae83-00c7b1725504")
        val kandidatutfall3 = kandidatutfallIPrioritertMålgruppe.copy(aktørId = "10", stillingsId = "a6f36cf5-f85e-451e-9212-cb23c1aac9ac")
        kandidatutfallRepository.lagreUtfall(kandidatutfall1)
        kandidatutfallRepository.lagreUtfall(kandidatutfall2)
        kandidatutfallRepository.lagreUtfall(kandidatutfall3)
        val nyttKandidatutfall1 = kandidatutfallIkkeIPrioritertMålgruppe.copy(aktørId = "11", stillingsId = kandidatutfall1.stillingsId)
        val nyttKandidatutfall2 = kandidatutfallIkkeIPrioritertMålgruppe.copy(aktørId = "12", stillingsId = kandidatutfall2.stillingsId)
        val nyttKandidatutfall3 = kandidatutfallIkkeIPrioritertMålgruppe.copy(aktørId = "13", stillingsId = kandidatutfall3.stillingsId)
        kandidatutfallRepository.lagreUtfall(nyttKandidatutfall1)
        kandidatutfallRepository.lagreUtfall(nyttKandidatutfall2)
        kandidatutfallRepository.lagreUtfall(nyttKandidatutfall3)
        visningKontaktinfoRepo.lagre(aktørId = nyttKandidatutfall1.aktørId, stillingsId = UUID.fromString(nyttKandidatutfall1.stillingsId), tidspunkt = nowOslo())
        visningKontaktinfoRepo.lagre(aktørId = nyttKandidatutfall2.aktørId, stillingsId = UUID.fromString(nyttKandidatutfall2.stillingsId), tidspunkt = nowOslo())
        visningKontaktinfoRepo.lagre(aktørId = nyttKandidatutfall3.aktørId, stillingsId = UUID.fromString(nyttKandidatutfall3.stillingsId), tidspunkt = nowOslo())
        val kandidatutfallSomSkalTelles = kandidatutfall1.copy(stillingsId = "688976b1-0340-42e1-b658-ba76eb437ed5")
        kandidatutfallRepository.lagreUtfall(kandidatutfallSomSkalTelles)
        visningKontaktinfoRepo.lagre(aktørId = kandidatutfallSomSkalTelles.aktørId, stillingsId = UUID.fromString(kandidatutfallSomSkalTelles.stillingsId), tidspunkt = nowOslo())

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
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
    fun `Kandidat med flere utfall på to lister og med vist kontaktinfo på begge, skal telles to ganger`() {
        val førsteKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo().minusHours(2))
        val andreKandidatuftall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = nowOslo().minusHours(1))
        val stillingsId = UUID.randomUUID().toString()
        val nyFørsteKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo().minusHours(2), stillingsId = stillingsId)
        val nyAndreKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = nowOslo().minusHours(1), stillingsId = stillingsId)
        kandidatutfallRepository.lagreUtfall(førsteKandidatutfall)
        kandidatutfallRepository.lagreUtfall(andreKandidatuftall)
        kandidatutfallRepository.lagreUtfall(nyFørsteKandidatutfall)
        kandidatutfallRepository.lagreUtfall(nyAndreKandidatutfall)
        visningKontaktinfoRepo.lagre(
            førsteKandidatutfall.aktørId,
            UUID.fromString(førsteKandidatutfall.stillingsId),
            tidspunkt = nowOslo().minusHours(2)
        )
        visningKontaktinfoRepo.lagre(
            førsteKandidatutfall.aktørId,
            UUID.fromString(førsteKandidatutfall.stillingsId),
            tidspunkt = nowOslo().minusHours(1)
        )
        visningKontaktinfoRepo.lagre(
            nyAndreKandidatutfall.aktørId,
            UUID.fromString(nyAndreKandidatutfall.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(2)
    }

    @Test
    fun `Kandidat med flere utfall på flere lister og med vist kontaktinfo på en, skal telles en gang`() {
        val førsteKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo().minusHours(2))
        val andreKandidatuftall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = nowOslo().minusHours(1))
        val tredjeKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo())
        val stillingsId = UUID.randomUUID().toString()
        val nyFørsteKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.PRESENTERT, tidspunktForHendelsen = nowOslo().minusHours(2), stillingsId = stillingsId)
        val nyAndreKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(utfall = Utfall.FATT_JOBBEN, tidspunktForHendelsen = nowOslo().minusHours(1), stillingsId = stillingsId)
        kandidatutfallRepository.lagreUtfall(førsteKandidatutfall)
        kandidatutfallRepository.lagreUtfall(andreKandidatuftall)
        kandidatutfallRepository.lagreUtfall(tredjeKandidatutfall)
        kandidatutfallRepository.lagreUtfall(nyFørsteKandidatutfall)
        kandidatutfallRepository.lagreUtfall(nyAndreKandidatutfall)
        visningKontaktinfoRepo.lagre(
            førsteKandidatutfall.aktørId,
            UUID.fromString(førsteKandidatutfall.stillingsId),
            tidspunkt = nowOslo().minusHours(2)
        )
        visningKontaktinfoRepo.lagre(
            førsteKandidatutfall.aktørId,
            UUID.fromString(førsteKandidatutfall.stillingsId),
            tidspunkt = nowOslo().minusHours(1)
        )
        visningKontaktinfoRepo.lagre(
            førsteKandidatutfall.aktørId,
            UUID.fromString(førsteKandidatutfall.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `kandidat har utfall på 2 lister, i prioritert målgruppe på den ene, men fått vist kontaktinfo på den andre`() {
        val kandidatutfall = kandidatutfallIkkeIPrioritertMålgruppe
        val nyttKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(aktørId = kandidatutfall.aktørId, stillingsId = UUID.randomUUID().toString())
        kandidatutfallRepository.lagreUtfall(kandidatutfall)
        kandidatutfallRepository.lagreUtfall(nyttKandidatutfall)
        visningKontaktinfoRepo.lagre(aktørId = kandidatutfall.aktørId, stillingsId = UUID.fromString(kandidatutfall.stillingsId), tidspunkt = nowOslo())

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `Skal ikke telle vist kontaktinfo når en kandidat ikke er i prioritert målgruppe, men en annen kandidat på lista er i prioritert målgruppe`() {
        val kandidatutfallIkkePrioritertMålgruppe = kandidatutfallIkkeIPrioritertMålgruppe
        val kandidatutfallPrioritertMålgruppe = kandidatutfallIPrioritertMålgruppe
        kandidatutfallRepository.lagreUtfall(kandidatutfallIkkePrioritertMålgruppe)
        kandidatutfallRepository.lagreUtfall(kandidatutfallPrioritertMålgruppe)
        visningKontaktinfoRepo.lagre(aktørId = kandidatutfallIkkePrioritertMålgruppe.aktørId, stillingsId = UUID.fromString(kandidatutfallIkkePrioritertMålgruppe.stillingsId), tidspunkt = nowOslo())

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(0)
    }

    @Test
    fun `En person som har fått vist sin kontaktinfo i en liste skal ikke telles flere ganger selvom han finnes på andre lister der han ikke har fått vist kontaktinfo`() {
        val kandidatutfall = kandidatutfallIPrioritertMålgruppe
        val nyttKandidatutfall = kandidatutfallIPrioritertMålgruppe.copy(stillingsId = UUID.randomUUID().toString())
        kandidatutfallRepository.lagreUtfall(kandidatutfall)
        kandidatutfallRepository.lagreUtfall(nyttKandidatutfall)
        visningKontaktinfoRepo.lagre(aktørId = kandidatutfall.aktørId, stillingsId = UUID.fromString(kandidatutfall.stillingsId), tidspunkt = nowOslo())

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)

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
    fun `Skal telle en gang for kandidat med utfall på forskjellige lister, men med åpnet kontaktinfo på en`() {
        val kandidatutfallPåEnListe =
            kandidatutfallIPrioritertMålgruppe.copy(aktørId = "10108000398", stillingsId = UUID.randomUUID().toString())
        val kandidatutfallPåEnAnnenListe =
            kandidatutfallIPrioritertMålgruppe.copy(aktørId = "10108000398", stillingsId = UUID.randomUUID().toString())

        kandidatutfallRepository.lagreUtfall(kandidatutfallPåEnListe)
        kandidatutfallRepository.lagreUtfall(kandidatutfallPåEnAnnenListe)

        visningKontaktinfoRepo.lagre(
            kandidatutfallPåEnListe.aktørId,
            UUID.fromString(kandidatutfallPåEnListe.stillingsId),
            tidspunkt = nowOslo()
        )

        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antall).isEqualTo(1)
    }

    @Test
    fun `Skal kunne telle mange kandidater med vist kontaktinfo`() {
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }
        uniktKandidatutfallIPrioritertMålgruppe().also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }

        kandidatutfallIkkeIPrioritertMålgruppe.also {
            kandidatutfallRepository.lagreUtfall(it)
            visningKontaktinfoRepo.lagre(it.aktørId, UUID.fromString(it.stillingsId), nowOslo())
        }

        val antallUtfall = kandidatutfallRepository.hentUsendteUtfall()
        val antall = visningKontaktinfoRepo.hentAntallKandidaterIPrioritertMålgruppeSomHarFåttVistSinKontaktinfo()

        assertThat(antallUtfall.size).isEqualTo(7)
        assertThat(antall).isEqualTo(6)
    }

    private val kandidatutfallIPrioritertMålgruppe =
        etKandidatutfall.copy(aktørId = "1", utfall = Utfall.PRESENTERT, harHullICv = true)

    private val kandidatutfallIkkeIPrioritertMålgruppe = etKandidatutfall.copy(
        aktørId = "2",
        utfall = Utfall.PRESENTERT,
        harHullICv = false,
        alder = 33,
        innsatsbehov = "IKVAL"
    )

    private fun uniktKandidatutfallIPrioritertMålgruppe() =
        kandidatutfallIPrioritertMålgruppe.copy(
            stillingsId = UUID.randomUUID().toString(),
            alder = 666,
            aktørId = UUID.randomUUID().toString().substring(26, 35)
        )
}