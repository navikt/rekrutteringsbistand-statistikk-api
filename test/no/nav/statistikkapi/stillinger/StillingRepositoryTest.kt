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
        val stilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 3, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 3, 3).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.ARBEIDSTID, InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS),
            tiltakEllerEllerVirkemidler = listOf(TiltakEllerVirkemiddelTag.LÆRLINGPLASS),
            stillingskategori = Stillingskategori.STILLING
        )

        repository.lagreStilling(stilling)
        val databaseStilling = repository.hentNyesteStilling(UUID.fromString(stilling.uuid))
            ?: throw IllegalStateException("Ingen stilling funnet i databasen med den UUID´en")

        assertThat(databaseStilling `er ulik` stilling).isFalse()
    }

    @Test
    fun `skal kunne hente en stilling basert på UUID`() {
        val stilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 3, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 3, 3).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.ARBEIDSTID, InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS),
            tiltakEllerEllerVirkemidler = listOf(TiltakEllerVirkemiddelTag.LÆRLINGPLASS),
            stillingskategori = Stillingskategori.STILLING
        )
        repository.lagreStilling(stilling)

        val lagretStilling = repository.hentNyesteStilling(UUID.fromString(stilling.uuid))

        assertThat(lagretStilling).isNotNull()
        assertThat(lagretStilling!!.uuid).isEqualTo(stilling.uuid)
        assertThat(lagretStilling.publisert).isEqualTo(stilling.publisert)
        assertThat(lagretStilling.inkluderingsmuligheter).isEqualTo(stilling.inkluderingsmuligheter)
        assertThat(lagretStilling.prioriterteMålgrupper).isEqualTo(stilling.prioriterteMålgrupper)
        assertThat(lagretStilling.tiltakEllerVirkemidler).isEqualTo(stilling.tiltakEllerEllerVirkemidler)
        assertThat(lagretStilling.stillingskategori).isEqualTo(stilling.stillingskategori)
        assertThat(lagretStilling.tidspunkt).isBetween(
            LocalDateTime.now().minusSeconds(2),
            LocalDateTime.now().plusSeconds(2)
        )
    }

    @After
    fun cleanUp() {
        slettAlleUtfall()
    }
}
