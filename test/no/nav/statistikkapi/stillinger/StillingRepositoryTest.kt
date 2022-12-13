package no.nav.statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import no.nav.statistikkapi.db.TestDatabase
import org.junit.After
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class StillingRepositoryTest {

    companion object {
        private val database = TestDatabase()
        private val repository = StillingRepository(database.dataSource)

        private fun slettAlleUtfall() {
            database.dataSource.connection.use {
                it.prepareStatement("DELETE FROM ${StillingRepository.stillingtabell}").execute()
            }
        }
    }

    @Test
    fun `skal lagre en stilling`() {
        val stillingsuuid = UUID.randomUUID().toString()
        repository.lagreStilling(
            stillingsuuid = stillingsuuid,
            stillingskategori = Stillingskategori.JOBBMESSE
        )
        val databaseStilling = repository.hentStilling(UUID.fromString(stillingsuuid))
            ?: throw IllegalStateException("Ingen stilling funnet i databasen med den UUID´en")

        assertThat(databaseStilling.stillingskategori).isEqualTo(Stillingskategori.JOBBMESSE)
    }

    @After
    fun cleanUp() {
        slettAlleUtfall()
    }
}
