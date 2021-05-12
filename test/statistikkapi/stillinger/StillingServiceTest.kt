package statistikkapi.stillinger

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.LocalDate
import java.util.*

class StillingServiceTest {

    private val stillingRepository = mockk<StillingRepository>()
    private val elasticSearchKlient = mockk<ElasticSearchKlient>()
    private val stillingService = StillingService(elasticSearchKlient, stillingRepository)

    @Test
    fun `Skal ikke lagre stilling fra ElasticSearch hvis den er lik stilling som allerede er lagret`() {
        val stillingFraElasticSearchOgFraDatabase = likStillingFraElasticSearchOgDatabase()
        val stillingFraElasticSearch = stillingFraElasticSearchOgFraDatabase.first
        val stillingFraDatabase = stillingFraElasticSearchOgFraDatabase.second
        every { elasticSearchKlient.hentStilling(any()) } returns stillingFraElasticSearch
        every { stillingRepository.hentStilling(any()) } returns stillingFraDatabase

        val stillingDatabaseId = stillingService.registrerStilling(UUID.randomUUID().toString())

        assertThat(stillingDatabaseId).isEqualTo(stillingFraDatabase.id)
        verify(exactly = 0) { stillingRepository.lagreStilling(any()) }
    }

    @Test
    fun `Skal lagre stilling fra ElasticSearch hvis stillingen har blitt endret fra sist den ble lagret`() {
        val likeStillingerFraELasticSearchOgFraDatabase = likStillingFraElasticSearchOgDatabase()
        val endretStillingFraElasticSearch =
            likeStillingerFraELasticSearchOgFraDatabase.first.copy(inkluderingsmuligheter = listOf(InkluderingTag.ARBEIDSTID, InkluderingTag.FYSISK))
        val stillingFraDatabase = likeStillingerFraELasticSearchOgFraDatabase.second
        every { elasticSearchKlient.hentStilling(any()) } returns endretStillingFraElasticSearch
        every { stillingRepository.hentStilling(any()) } returns stillingFraDatabase
        val databaseIdForNyLagretStilling = 10L
        every { stillingRepository.lagreStilling(endretStillingFraElasticSearch) } returns databaseIdForNyLagretStilling

        val stillingDatabaseId = stillingService.registrerStilling(UUID.randomUUID().toString())

        assertThat(stillingDatabaseId).isNotEqualTo(stillingFraDatabase)
        assertThat(stillingDatabaseId).isEqualTo(databaseIdForNyLagretStilling)
        verify(exactly = 1) { stillingRepository.lagreStilling(endretStillingFraElasticSearch) }
    }

    @Test
    fun `Skal lagre stilling fra ElasticSearch hvis stillingen ikke finnes i databasen`() {
        val stillingFraElasticSearch = likStillingFraElasticSearchOgDatabase().first
        every { elasticSearchKlient.hentStilling(any()) } returns stillingFraElasticSearch
        every { stillingRepository.hentStilling(any()) } returns null
        every { stillingRepository.lagreStilling(stillingFraElasticSearch) } returns 1L

        val stillingDatabaseId = stillingService.registrerStilling(UUID.randomUUID().toString())

        assertThat(stillingDatabaseId).isNotNull()
        verify(exactly = 1) { stillingRepository.lagreStilling(stillingFraElasticSearch) }
    }

    @Test
    fun `Skal returnere ID på stilling i databasen dersom stilling ikke finnes i ElasticSearch`() {
        val stillingFraDatabasen = likStillingFraElasticSearchOgDatabase().second
        every { elasticSearchKlient.hentStilling(any()) } returns null
        every { stillingRepository.hentStilling(any()) } returns stillingFraDatabasen

        val stillingDatabaseId = stillingService.registrerStilling(UUID.randomUUID().toString())

        assertThat(stillingDatabaseId).isEqualTo(stillingFraDatabasen.id)
        verify(exactly = 0) { stillingRepository.lagreStilling(any()) }
    }

    @Test(expected = RuntimeException::class)
    fun `Dersom man ikke får treff på stillingUuid i hverken database eller ElasticSearch skal feil kastes`() {
        every { elasticSearchKlient.hentStilling(any()) } returns null
        every { stillingRepository.hentStilling(any()) } returns null

        stillingService.registrerStilling(UUID.randomUUID().toString())
    }

    private fun likStillingFraElasticSearchOgDatabase(): Pair<ElasticSearchStilling, Stilling> {
        val elasticSearchStilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 5, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 5, 2).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS, PrioriterteMålgrupperTag.HULL_I_CV),
            tiltakEllerEllerVirkemidler = emptyList()
        )
        val stillingFraDatabase = Stilling(
            id = 1,
            uuid = elasticSearchStilling.uuid,
            opprettet = elasticSearchStilling.opprettet,
            publisert = elasticSearchStilling.publisert,
            inkluderingsmuligheter = elasticSearchStilling.inkluderingsmuligheter,
            prioriterteMålgrupper = elasticSearchStilling.prioriterteMålgrupper,
            tiltakEllerEllerVirkemidler = emptyList(),
            tidspunkt = LocalDate.of(2021, 5, 4).atStartOfDay()
        )
        return Pair(elasticSearchStilling, stillingFraDatabase)
    }
}
