package statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.After
import org.junit.Test
import statistikkapi.db.TestDatabase
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
    fun `skal lagre en stilling og returnere ID`() {
        val stilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 3, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 3, 3).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.ARBEIDSTID, InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS),
            tiltakEllerEllerVirkemidler = listOf(TiltakEllerVirkemiddelTag.LÆRLINGPLASS))

        val databaseId = repository.lagreStilling(stilling)

        assertThat(databaseId).isNotNull()
    }

    @Test
    fun `skal kunne hente en stilling basert på UUID`() {
        val stilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 3, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 3, 3).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.ARBEIDSTID, InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS),
            tiltakEllerEllerVirkemidler = listOf(TiltakEllerVirkemiddelTag.LÆRLINGPLASS))
        repository.lagreStilling(stilling)

        val lagretStilling = repository.hentStilling(stilling.uuid)

        assertThat(lagretStilling).isNotNull()
        assertThat(lagretStilling!!.uuid).isEqualTo(stilling.uuid)
        assertThat(lagretStilling.publisert).isEqualTo(stilling.publisert)
        assertThat(lagretStilling.inkluderingsmuligheter).isEqualTo(stilling.inkluderingsmuligheter)
        assertThat(lagretStilling.prioriterteMålgrupper).isEqualTo(stilling.prioriterteMålgrupper)
        assertThat(lagretStilling.tiltakEllerEllerVirkemidler).isEqualTo(stilling.tiltakEllerEllerVirkemidler)
        assertThat(lagretStilling.tidspunkt).isBetween(LocalDateTime.now().minusSeconds(2), LocalDateTime.now().plusSeconds(2))
    }

    @After
    fun cleanUp() {
        slettAlleUtfall()
    }
}
