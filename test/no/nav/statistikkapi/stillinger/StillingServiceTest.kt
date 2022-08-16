package no.nav.statistikkapi.stillinger

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.LocalDate
import java.util.*

class StillingServiceTest {

    private val stillingRepository = mockk<StillingRepository>()
    private val stillingEsKlient = mockk<StillingEsKlient>()
    private val stillingService = StillingService(stillingEsKlient, stillingRepository)

    @Test
    fun `Skal ikke lagre stilling fra ElasticSearch hvis den er lik stilling som allerede er lagret`() {
        val stillingFraElasticSearchOgFraDatabase = likStillingFraElasticSearchOgDatabase()
        val stillingFraElasticSearch = stillingFraElasticSearchOgFraDatabase.first
        val stillingFraDatabase = stillingFraElasticSearchOgFraDatabase.second
        every { stillingEsKlient.hentStilling(any()) } returns stillingFraElasticSearch
        every { stillingRepository.hentNyesteStilling(any()) } returns stillingFraDatabase

        stillingService.registrerOgHent(UUID.randomUUID())

        verify(exactly = 0) { stillingRepository.lagreStilling(any()) }
    }

    @Test
    fun `Skal lagre stilling fra ElasticSearch hvis stillingen har blitt endret fra sist den ble lagret`() {
        val likeStillingerFraELasticSearchOgFraDatabase = likStillingFraElasticSearchOgDatabase()
        val endretStillingFraElasticSearch =
            likeStillingerFraELasticSearchOgFraDatabase.first.copy(
                inkluderingsmuligheter = listOf(
                    InkluderingTag.ARBEIDSTID,
                    InkluderingTag.FYSISK
                )
            )
        val stillingFraDatabase = likeStillingerFraELasticSearchOgFraDatabase.second
        every { stillingEsKlient.hentStilling(any()) } returns endretStillingFraElasticSearch
        every { stillingRepository.hentNyesteStilling(any()) } returns stillingFraDatabase
        justRun { stillingRepository.lagreStilling(endretStillingFraElasticSearch) }

        stillingService.registrerOgHent(UUID.randomUUID())

        verify(exactly = 1) { stillingRepository.lagreStilling(endretStillingFraElasticSearch) }
    }

    @Test
    fun `Skal lagre stilling fra ElasticSearch hvis stillingen ikke finnes i databasen`() {
        val stilling = likStillingFraElasticSearchOgDatabase()
        val stillingFraElasticSearch = stilling.first
        every { stillingEsKlient.hentStilling(any()) } returns stillingFraElasticSearch
        every { stillingRepository.hentNyesteStilling(any()) } returns null andThen stilling.second
        justRun { stillingRepository.lagreStilling(stillingFraElasticSearch) }

        stillingService.registrerOgHent(UUID.randomUUID())

        verify(exactly = 1) { stillingRepository.lagreStilling(stillingFraElasticSearch) }
    }

    @Test(expected = RuntimeException::class)
    fun `Dersom man ikke får treff på stillingUuid i hverken database eller ElasticSearch skal feil kastes`() {
        every { stillingEsKlient.hentStilling(any()) } returns null
        every { stillingRepository.hentNyesteStilling(any()) } returns null

        stillingService.registrerOgHent(UUID.randomUUID())
    }

    private fun likStillingFraElasticSearchOgDatabase(): Pair<ElasticSearchStilling, Stilling> {
        val elasticSearchStilling = ElasticSearchStilling(
            uuid = UUID.randomUUID().toString(),
            opprettet = LocalDate.of(2021, 5, 2).atStartOfDay(),
            publisert = LocalDate.of(2021, 5, 2).atStartOfDay(),
            inkluderingsmuligheter = listOf(InkluderingTag.FYSISK),
            prioriterteMålgrupper = listOf(
                PrioriterteMålgrupperTag.KOMMER_FRA_LAND_UTENFOR_EØS,
                PrioriterteMålgrupperTag.HULL_I_CV_EN
            ),
            tiltakEllerEllerVirkemidler = emptyList(),
            stillingskategori = Stillingskategori.STILLING
        )
        val stillingFraDatabase = Stilling(
            uuid = elasticSearchStilling.uuid,
            opprettet = elasticSearchStilling.opprettet,
            publisert = elasticSearchStilling.publisert,
            inkluderingsmuligheter = elasticSearchStilling.inkluderingsmuligheter,
            prioriterteMålgrupper = elasticSearchStilling.prioriterteMålgrupper,
            tiltakEllerVirkemidler = emptyList(),
            tidspunkt = LocalDate.of(2021, 5, 4).atStartOfDay(),
            stillingskategori = Stillingskategori.STILLING
        )
        return Pair(elasticSearchStilling, stillingFraDatabase)
    }
}
