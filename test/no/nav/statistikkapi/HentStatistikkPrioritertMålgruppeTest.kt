package no.nav.statistikkapi

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.statistikkapi.db.TestDatabase
import no.nav.statistikkapi.db.TestRepository
import no.nav.statistikkapi.kandidatutfall.Innsatsgruppe
import no.nav.statistikkapi.kandidatutfall.KandidatutfallRepository
import no.nav.statistikkapi.kandidatutfall.Utfall.*
import org.junit.After
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class HentStatistikkPrioritertMålgruppeTest {


    /*  Tester både presentasjoner og fått jobben i samme testfunksjon, fordi setup, handlig og resultat skal være likt

 // Ung, under 30
 Gitt utfall
     presentert 29
     fått jobben 29
 når henter statistikk
 så skal
  ant. pres. i målgr == 1
  ant. fått jobb i målgr == 1

 // Ikk eung, 30+
 Gitt utfall
     presentert 30
     fått jobben 30
 når henter statistikk
 så skal
  ant. pres. i målgr == 0
  ant. fått jobb i målgr == 0


 // Senior, 50+
 Gitt utfall
     presentert 50
     fått jobben 50
 når henter statistikk
 så skal
  ant. pres. i målgr == 1
  ant. fått jobb i målgr == 1

 // Ikke senior, under 50
 Gitt utfall
     presentert 49
     fått jobben 49
 når henter statistikk
 så skal
  ant. pres. i målgr == 0
  ant. fått jobb i målgr == 0


 // Innsatsgruppe kvalifiserer
 Gitt utfall
     presentert, inngr. BATT
     presentert, inngr. BFORM
     presentert, inngr. VARIG
     fått jobben, inngr. BATT
     fått jobben, inngr. BFORM
     fått jobben, inngr. VARIG
 når henter statistikk
 så skal
      ant. pres. i målgr == 3
      ant. fått jobb i målgr == 3

 // Innsatsgruppe kvalifiserer ikke
 Gitt utfall
     presentert, inngr. null
     presentert, inngr. IKVAL
     fått jobben, inngr. null
     fått jobben, inngr. IKVAL
 når henter statistikk
 så skal
      ant. pres. i målgr == 0
      ant. fått jobb i målgr == 0


 // Kombinasjon både ung og innsatsgruppe kvalifiserer
 Gitt utfall
     presentert, inngr. BATT, alder 29
     fått jobben, inngr. BATT, alder 29
 når henter statistikk
 så skal
      ant. pres. i målgr == 1
      ant. fått jobb i målgr == 1


 // Kombinasjon både senior og innsatsgruppe kvalifiserer
 Gitt utfall
     presentert, inngr. BATT, alder 50
     fått jobben, inngr. BATT, alder 50
 når henter statistikk
 så skal
      ant. pres. i målgr == 1
      ant. fått jobb i målgr == 1
  */





    companion object {
        private val port = randomPort()
        private val mockOAuth2Server = MockOAuth2Server()
        private val client = httpKlientMedBearerToken(mockOAuth2Server)
        private val basePath = basePath(port)
        private val database = TestDatabase()
        private val repository = KandidatutfallRepository(database.dataSource)
        private val testRepository = TestRepository(database.dataSource)

        init {
            start(
                database = database,
                port = port,
                mockOAuth2Server = mockOAuth2Server
            )
        }

        fun lagTidspunkt(year: Int, month: Int, day: Int): ZonedDateTime =
            LocalDate.of(year, month, day).atStartOfDay().atOslo()

        private fun leggTilQueryParametere(
            httpRequestBuilder: HttpRequestBuilder,
            fraOgMed: LocalDate,
            tilOgMed: LocalDate,
            navKontor: String
        ) {
            httpRequestBuilder.url.parameters.apply {
                append(StatistikkParametere.fraOgMed, fraOgMed.toString())
                append(StatistikkParametere.tilOgMed, tilOgMed.toString())
                append(StatistikkParametere.navKontor, navKontor)
            }
        }

        private fun hentStatistikk(
            fraOgMed: LocalDate,
            tilOgMed: LocalDate,
            navKontor: String
        ): StatistikkOutboundDto = runBlocking {
            client.get("$basePath/statistikk") {
                leggTilQueryParametere(this, fraOgMed, tilOgMed, navKontor)
            }.body()
        }

    }


    @After
    fun cleanUp() {
        testRepository.slettAlleUtfall()
        mockOAuth2Server.shutdown()
    }
}
